package com.realestate

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.realestate.domain.{Listing, NotificationSubscription, PropertyType, PropertyTypeJsonProtocol}
import com.realestate.events.{ListingCreated, ListingPriceChanged, ListingPropertyTypeChanged}
import spray.json._
import com.realestate.events.EventJsonProtocol._
import com.realestate.domain.{ListingJsonProtocol, NotificationSubscriptionJsonProtocol}
import com.realestate.domain.ListingJsonProtocol._
import com.realestate.domain.NotificationSubscriptionJsonProtocol._
import com.realestate.domain.PropertyTypeJsonProtocol._

import spray.json._
import PropertyTypeJsonProtocol._

import java.util.UUID

class RealEstatePocSpec extends AnyFlatSpec with Matchers {

  "PropertyType" should "parse valid property types correctly" in {
    PropertyType.fromString("RESIDENTIAL") shouldBe Some(PropertyType.Residential)
    PropertyType.fromString("COMMERCIAL") shouldBe Some(PropertyType.Commercial)
    PropertyType.fromString("INDUSTRIAL") shouldBe Some(PropertyType.Industrial)
    PropertyType.fromString("LAND") shouldBe Some(PropertyType.Land)
    PropertyType.fromString("MIXED_USE") shouldBe Some(PropertyType.MixedUse)
    PropertyType.fromString("INVESTMENT") shouldBe Some(PropertyType.Investment)
  }

  "PropertyType" should "return None for invalid property types" in {
    PropertyType.fromString("INVALID") shouldBe None
    PropertyType.fromString("") shouldBe None
  }

//  "PropertyType" should "serialize and deserialize correctly" in {
//    val propertyType = PropertyType.Commercial
//    val json = propertyType.toJson
//    val deserialized = json.convertTo[PropertyType]
//
//    deserialized shouldBe propertyType
//  }

  "Listing" should "serialize and deserialize correctly with property type" in {
    val id = UUID.randomUUID()
    val listing = Listing(id, "123 Main St", 500000, PropertyType.Commercial)
    
    val json = listing.toJson
    val deserialized = json.convertTo[Listing]
    
    deserialized shouldBe listing
  }

  "Listing" should "use Residential as default property type" in {
    val id = UUID.randomUUID()
    val listing = Listing(id, "123 Main St", 500000)
    
    listing.propertyType shouldBe PropertyType.Residential
  }

  "NotificationSubscription" should "serialize and deserialize correctly" in {
    val id = UUID.randomUUID()
    val subscription = NotificationSubscription(id, "user123", "Main St", 600000)
    
    val json = subscription.toJson
    val deserialized = json.convertTo[NotificationSubscription]
    
    deserialized shouldBe subscription
  }

  "ListingCreated event" should "serialize and deserialize correctly with property type" in {
    val id = UUID.randomUUID()
    val event = ListingCreated(id, "123 Main St", 500000, PropertyType.Industrial)
    
    val json = event.toJson
    val deserialized = json.convertTo[ListingCreated]
    
    deserialized.id shouldBe event.id
    deserialized.address shouldBe event.address
    deserialized.price shouldBe event.price
    deserialized.propertyType shouldBe event.propertyType
  }

  "ListingPriceChanged event" should "serialize and deserialize correctly" in {
    val id = UUID.randomUUID()
    val event = ListingPriceChanged(id, 500000, 450000)
    
    val json = event.toJson
    val deserialized = json.convertTo[ListingPriceChanged]
    
    deserialized.id shouldBe event.id
    deserialized.oldPrice shouldBe event.oldPrice
    deserialized.newPrice shouldBe event.newPrice
  }

  "ListingPropertyTypeChanged event" should "serialize and deserialize correctly" in {
    val id = UUID.randomUUID()
    val event = ListingPropertyTypeChanged(id, PropertyType.Residential, PropertyType.Commercial)
    
    val json = event.toJson
    val deserialized = json.convertTo[ListingPropertyTypeChanged]
    
    deserialized.id shouldBe event.id
    deserialized.oldPropertyType shouldBe event.oldPropertyType
    deserialized.newPropertyType shouldBe event.newPropertyType
  }
}
