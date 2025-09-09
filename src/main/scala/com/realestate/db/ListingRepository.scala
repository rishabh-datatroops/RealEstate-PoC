package com.realestate.db

import com.realestate.domain.{Listing, PropertyType}
import com.realestate.events.EventJsonProtocol._
import com.realestate.events.{ListingCreated, ListingPriceChanged, ListingPropertyTypeChanged}
import slick.jdbc.PostgresProfile.api._
import spray.json._

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ListingRepository(db: Database)(implicit ec: ExecutionContext) {
  private val listings = TableQuery[ListingTable]
  private val outbox = TableQuery[OutboxTable]

  def createListing(listing: Listing): Future[Unit] = {
    val event = ListingCreated(listing.id, listing.address, listing.price, listing.propertyType)
    val row = OutboxRow(
      UUID.randomUUID(),
      "listings.events",
      listing.id.toString,
      event.toJson.compactPrint,
      Instant.now
    )

    val action = DBIO.seq(
      listings += listing,
      outbox += row
    )

    db.run(action.transactionally)
  }

  def getListing(id: UUID): Future[Option[Listing]] = {
    db.run(listings.filter(_.id === id).result.headOption)
  }

  def updatePrice(id: UUID, newPrice: Long): Future[Unit] = {
    val q = listings.filter(_.id === id).result.headOption

    val action = q.flatMap {
      case Some(listing) =>
        val event = ListingPriceChanged(listing.id, listing.price, newPrice)
        val row = OutboxRow(
          UUID.randomUUID(),
          "listings.events",
          listing.id.toString,
          event.toJson.compactPrint,
          Instant.now
        )
        DBIO.seq(
          listings.filter(_.id === id).map(_.price).update(newPrice), // FIXED
          outbox += row
        )

      case None =>
        DBIO.failed(new NoSuchElementException(s"Listing $id not found"))
    }

    db.run(action.transactionally)
  }

  def updatePropertyType(id: UUID, newPropertyType: PropertyType): Future[Unit] = {
    val q = listings.filter(_.id === id).result.headOption

    val action = q.flatMap {
      case Some(listing) =>
        val event = ListingPropertyTypeChanged(listing.id, listing.propertyType, newPropertyType)
        val row = OutboxRow(
          UUID.randomUUID(),
          "listings.events",
          listing.id.toString,
          event.toJson.compactPrint,
          Instant.now
        )
        DBIO.seq(
          listings.filter(_.id === id).map(_.propertyTypeStr).update(newPropertyType.name),
          outbox += row
        )

      case None =>
        DBIO.failed(new NoSuchElementException(s"Listing $id not found"))
    }

    db.run(action.transactionally)
  }

  def allListings(): Future[Seq[Listing]] =
    db.run(listings.result)

  def findListingsByPropertyType(propertyType: PropertyType): Future[Seq[Listing]] = {
    db.run(listings.filter(_.propertyTypeStr === propertyType.name).result)
  }

}
