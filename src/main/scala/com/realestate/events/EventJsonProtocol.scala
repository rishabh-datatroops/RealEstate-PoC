package com.realestate.events
import spray.json._
import java.util.UUID
import java.time.Instant
import com.realestate.domain.{PropertyType, PropertyTypeJsonProtocol}

object EventJsonProtocol extends DefaultJsonProtocol {
  import PropertyTypeJsonProtocol._
  
  implicit val uuidFormat: JsonFormat[UUID] = new JsonFormat[UUID] {
    override def write(uuid: UUID): JsValue = JsString(uuid.toString)
    override def read(value: JsValue): UUID = value match {
      case JsString(s) => UUID.fromString(s)
      case other => deserializationError(s"Expected UUID as JsString, but got: $other")
    }
  }

  implicit val instantFormat: JsonFormat[Instant] = new JsonFormat[Instant] {
    override def write(instant: Instant): JsValue = JsString(instant.toString)
    override def read(value: JsValue): Instant = value match {
      case JsString(s) => Instant.parse(s)
      case other => deserializationError(s"Expected Instant as JsString (ISO-8601), but got: $other")
    }
  }

  implicit val listingCreatedFormat: RootJsonFormat[ListingCreated] =
    jsonFormat5(ListingCreated)

  implicit val listingPriceChangedFormat: RootJsonFormat[ListingPriceChanged] =
    jsonFormat4(ListingPriceChanged)
    
  implicit val listingPropertyTypeChangedFormat: RootJsonFormat[ListingPropertyTypeChanged] =
    jsonFormat4(ListingPropertyTypeChanged)
}
