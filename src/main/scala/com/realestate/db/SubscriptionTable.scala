package com.realestate.db

import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import java.time.Instant
import com.realestate.domain.NotificationSubscription

case class SubscriptionRow(
  id: UUID,
  userId: String,
  address: String,
  price: Long,
  createdAt: Instant
)

class SubscriptionTable(tag: Tag) extends Table[SubscriptionRow](tag, "subscriptions") {
  def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)
  def userId: Rep[String] = column[String]("user_id")
  def address: Rep[String] = column[String]("address")
  def price: Rep[Long] = column[Long]("price")
  def createdAt: Rep[Instant] = column[Instant]("created_at")

  def * = (id, userId, address, price, createdAt) <> (SubscriptionRow.tupled, SubscriptionRow.unapply)
}

