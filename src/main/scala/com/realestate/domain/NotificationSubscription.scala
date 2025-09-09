package com.realestate.domain

import java.util.UUID
import spray.json._

case class NotificationSubscription(
  id: UUID,
  userId: String,
  address: String,
  price: Long
)

object NotificationSubscriptionJsonProtocol extends DefaultJsonProtocol {
  implicit val uuidFormat: JsonFormat[UUID] = new JsonFormat[UUID] {
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
    override def read(value: JsValue): UUID = value match {
      case JsString(s) => UUID.fromString(s)
      case other => deserializationError(s"Expected UUID as JsString, but got: $other")
    }
  }

  implicit val subscriptionFormat: RootJsonFormat[NotificationSubscription] = jsonFormat4(NotificationSubscription)
}
