package com.realestate.db

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import java.util.UUID
import com.realestate.domain.{Listing, PropertyType}

class ListingTable(tag: Tag) extends Table[Listing](tag, "listings") {
  def id: Rep[UUID]        = column[UUID]("id", O.PrimaryKey)
  def address: Rep[String] = column[String]("address")
  def price: Rep[Long]     = column[Long]("price")
  def propertyTypeStr: Rep[String] = column[String]("property_type")

  def * : ProvenShape[Listing] =
    (id, address, price, propertyTypeStr).<>(
      { case (id: UUID, address: String, price: Long, pt: String) =>
        Listing(id, address, price, PropertyType.fromStringOrThrow(pt))
      },
      (l: Listing) => Some((l.id, l.address, l.price, l.propertyType.name))
    )
}