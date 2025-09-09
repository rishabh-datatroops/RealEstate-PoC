package com.realestate.domain

import java.util.UUID
import spray.json._

case class Listing(id: UUID, address: String, price: Long, propertyType: PropertyType = PropertyType.Residential)

object ListingJsonProtocol extends DefaultJsonProtocol {
  import PropertyTypeJsonProtocol._
  
  implicit val uuidFormat: JsonFormat[UUID] = new JsonFormat[UUID] {
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
    override def read(value: JsValue): UUID = value match {
      case JsString(s) => UUID.fromString(s)
      case other => deserializationError(s"Expected UUID as JsString, but got: $other")
    }
  }

  implicit val listingFormat: RootJsonFormat[Listing] = jsonFormat4(Listing)
}
