package com.realestate.producer

import akka.actor.ActorSystem
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Sink, Source}
import com.realestate.db.OutboxTable
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class OutboxPublisher(db: Database, topic: String)(implicit system: ActorSystem) {
  private val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

  private val outbox = TableQuery[OutboxTable]

  def start(): Unit = {
    import system.dispatcher

    system.scheduler.scheduleWithFixedDelay(0.seconds, 5.seconds) { () =>
      // Fetch messages in small batches
      val rowsF = db.run(outbox.sortBy(_.id).take(100).result)

      rowsF.foreach { rows =>
        if (rows.nonEmpty) {
          Source(rows.toList)
            .map { row =>
              ProducerMessage.single(
                new ProducerRecord[String, String](row.topic, row.key, row.payload),
                row.id // pass through id for deletion
              )
            }
            .via(Producer.flexiFlow(producerSettings))
            .mapAsync(1) {
              case result: ProducerMessage.Result[String, String, UUID] =>
                val id = result.passThrough
                val metadata = result.metadata
                db.run(outbox.filter(_.id === id).delete)
                  .map(_ => println(s"[OutboxPublisher] Published and deleted message $id to ${metadata.topic()}"))
          case other =>
                Future.failed(new RuntimeException(s"Unexpected result: $other"))
            }
            .runWith(Sink.ignore)
            .onComplete {
              case Success(_) =>
                println(s"[OutboxPublisher] Successfully processed batch of ${rows.size} messages")
              case Failure(ex) =>
                println(s"[OutboxPublisher] Error in publishing stream: ${ex.getMessage}")
            }
        }
      }
    }
  }
}
