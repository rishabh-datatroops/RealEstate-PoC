package com.realestate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.realestate.db.{ListingRepository, NotificationRepository}
import com.realestate.producer.OutboxPublisher
import com.realestate.consumer.SearchProjector
import com.realestate.api.{ListingRoutes, NotificationRoutes}
import com.realestate.service.NotificationService
import slick.jdbc.PostgresProfile.api._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Success, Failure}

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("real-estate-poc")
  implicit val ec: ExecutionContext = system.dispatcher

  val db = Database.forConfig("db")

  val listingRepo = new ListingRepository(db)
  val notificationRepo = new NotificationRepository(db)

  val publisher = new OutboxPublisher(db, "listings.events")
  publisher.start()

  val projector = new SearchProjector("listings.events")
  projector.start()

  val notificationService = new NotificationService(notificationRepo)
  notificationService.startConsumer()

  val listingRoutes = new ListingRoutes(listingRepo, projector).routes
  val notificationRoutes = new NotificationRoutes(notificationRepo, notificationService).routes

  val allRoutes: Route = listingRoutes ~ notificationRoutes

  val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(allRoutes)

  bindingFuture.onComplete {
    case Success(binding) =>
      println(s"Server running at http://localhost:${binding.localAddress.getPort}")
    case Failure(ex) =>
      println(s"Failed to bind server: ${ex.getMessage}")
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
