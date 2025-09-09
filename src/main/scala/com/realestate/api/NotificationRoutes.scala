package com.realestate.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.realestate.api.SharedJsonProtocol._
import com.realestate.db.NotificationRepository
import com.realestate.domain.NotificationSubscription
import com.realestate.domain.NotificationSubscriptionJsonProtocol._
import com.realestate.service.{NotificationService, NotificationAlert}
import spray.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class SubscriptionRequest(userId: String, address: String, price: Long)

object SubscriptionJsonProtocol extends DefaultJsonProtocol {
  implicit val reqFormat = jsonFormat3(SubscriptionRequest)
  
  // JSON format for LocalDateTime
  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {
    def write(dt: LocalDateTime): JsValue = {
      JsString(dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
    def read(value: JsValue): LocalDateTime = value match {
      case JsString(dt) => LocalDateTime.parse(dt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      case _ => throw new DeserializationException("Expected ISO LocalDateTime as JsString")
    }
  }
  
  // JSON format for NotificationAlert
  implicit val notificationAlertFormat = jsonFormat5(NotificationAlert)
}

class NotificationRoutes(repo: NotificationRepository, notificationService: NotificationService)(implicit ec: ExecutionContext) {
  import SubscriptionJsonProtocol._

  val routes: Route =
    pathPrefix("subscriptions") {
      concat(
        pathEndOrSingleSlash {
          concat(
            post {
              entity(as[SubscriptionRequest]) { req =>
                val sub = NotificationSubscription(
                  id = UUID.randomUUID(),
                  userId = req.userId,
                  address = req.address,
                  price = req.price
                )

                onComplete(repo.addSubscription(sub)) {
                  case Success(_) =>
                    complete(
                      StatusCodes.Created,
                      ApiResponse(
                        message = "Subscription created successfully",
                        data = Some(sub.toJson)
                      )
                    )
                  case Failure(ex: IllegalArgumentException) =>
                    complete(StatusCodes.BadRequest, ApiResponse(s"Invalid subscription data: ${ex.getMessage}"))
                  case Failure(ex) =>
                    complete(StatusCodes.InternalServerError, ApiResponse(s"Error creating subscription: ${ex.getMessage}"))
                }
              }
            },
            get {
              onSuccess(repo.allSubscriptions()) { subscriptions =>
                complete(ApiResponse(
                  message = "Subscriptions retrieved successfully",
                  data = Some(JsArray(subscriptions.map(_.toJson).toVector))
                ))
              }
            }
          )
        },
        path(JavaUUID) { id =>
          concat(
            get {
              onSuccess(repo.getSubscription(id)) {
                case Some(subscription) => complete(ApiResponse(
                  message = "Subscription retrieved successfully",
                  data = Some(subscription.toJson)
                ))
                case None => complete(StatusCodes.NotFound, ApiResponse("Subscription not found"))
              }
            },
            delete {
              onSuccess(repo.deleteSubscription(id)) { deletedCount =>
                if (deletedCount > 0) {
                  complete(ApiResponse("Subscription deleted successfully"))
                } else {
                  complete(StatusCodes.NotFound, ApiResponse("Subscription not found"))
                }
              }
            }
          )
        },
        path("user" / Segment) { userId =>
          get {
            onSuccess(repo.findSubscriptionsByUserId(userId)) { subscriptions =>
              complete(ApiResponse(
                message = "User subscriptions retrieved successfully",
                data = Some(JsArray(subscriptions.map(_.toJson).toVector))
              ))
            }
          }
        }
      )
    } ~
    pathPrefix("notifications") {
      concat(
        pathEndOrSingleSlash {
          get {
            val notifications = notificationService.getRecentNotifications()
            complete(ApiResponse(
              message = s"Retrieved ${notifications.length} recent notifications",
              data = Some(JsArray(notifications.map(_.toJson).toVector))
            ))
          }
        },
        path("user" / Segment) { userId =>
          get {
            val notifications = notificationService.getRecentNotifications()
              .filter(_.userId == userId)
            complete(ApiResponse(
              message = s"Retrieved ${notifications.length} notifications for user $userId",
              data = Some(JsArray(notifications.map(_.toJson).toVector))
            ))
          }
        }
      )
    }
}
