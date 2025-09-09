package com.realestate.events

import java.util.UUID
import java.time.Instant
import com.realestate.domain.PropertyType

sealed trait ListingEvent

case class ListingCreated(
  id: UUID, 
  address: String, 
  price: Long, 
  propertyType: PropertyType,
  createdAt: Instant = Instant.now
) extends ListingEvent

case class ListingPriceChanged(
  id: UUID, 
  oldPrice: Long, 
  newPrice: Long, 
  changedAt: Instant = Instant.now
) extends ListingEvent

case class ListingPropertyTypeChanged(
  id: UUID,
  oldPropertyType: PropertyType,
  newPropertyType: PropertyType,
  changedAt: Instant = Instant.now
) extends ListingEvent
