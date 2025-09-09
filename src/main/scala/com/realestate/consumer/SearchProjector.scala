package com.realestate.consumer

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.common.serialization.StringDeserializer
import com.realestate.events.{EventJsonProtocol, ListingCreated, ListingPriceChanged, ListingPropertyTypeChanged}
import spray.json._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class SearchProjector(topic: String)(implicit system: ActorSystem) {
  import EventJsonProtocol._

  case class ListingInfo(address: String, propertyType: String, price: Long)
  val index: TrieMap[String, ListingInfo] = TrieMap[String, ListingInfo]()

  def start()(implicit ex : ExecutionContext): Unit = {
    val settings =
      ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
        .withBootstrapServers("localhost:9092")
        .withGroupId("search-projector")
        .withProperty("auto.offset.reset", "earliest")

    Consumer
      .plainSource(settings, Subscriptions.topics(topic))
      .map { record =>
        try {
          val eventJson = record.value().parseJson
          try {
            val event = eventJson.convertTo[ListingCreated]
            println(s"[SearchProjector] ListingCreated: ${event.id} at ${event.address} (${event.propertyType.name})")
            index.put(event.id.toString, ListingInfo(event.address, event.propertyType.name, event.price))
          } catch {
            case _: spray.json.DeserializationException =>
              try {
                val event = eventJson.convertTo[ListingPriceChanged]
                println(s"[SearchProjector] ListingPriceChanged: ${event.id} - ${event.oldPrice} -> ${event.newPrice}")
                // Update price in existing listing info
                index.get(event.id.toString).foreach { info =>
                  index.put(event.id.toString, info.copy(price = event.newPrice))
                }
              } catch {
                case _: spray.json.DeserializationException =>
                  try {
                    val event = eventJson.convertTo[ListingPropertyTypeChanged]
                    println(s"[SearchProjector] ListingPropertyTypeChanged: ${event.id} - ${event.oldPropertyType.name} -> ${event.newPropertyType.name}")

                    index.get(event.id.toString).foreach { info =>
                      index.put(event.id.toString, info.copy(propertyType = event.newPropertyType.name))
                    }
                  } catch {
                    case ex: spray.json.DeserializationException =>
                      println(s"[SearchProjector] Could not parse event: ${ex.getMessage}")
                  }
              }
          }
        } catch {
          case ex: Exception =>
            println(s"[SearchProjector] Error processing record: ${ex.getMessage}")
        }
      }
      .runForeach(_ => ())
      .onComplete {
        case Success(_) => println("[SearchProjector] Consumer completed")
        case Failure(ex) => println(s"[SearchProjector] Consumer failed: ${ex.getMessage}")
      }
  }

  def search(q: String): Seq[ListingInfo] = {
    index.values.filter(_.address.toLowerCase.contains(q.toLowerCase)).toSeq
  }
  
  def searchByPropertyType(propertyType: String): Seq[ListingInfo] = {
    index.values.filter(_.propertyType == propertyType.toUpperCase).toSeq
  }
  
  def searchByPriceRange(minPrice: Long, maxPrice: Long): Seq[ListingInfo] = {
    index.values.filter(info => info.price >= minPrice && info.price <= maxPrice).toSeq
  }
}
