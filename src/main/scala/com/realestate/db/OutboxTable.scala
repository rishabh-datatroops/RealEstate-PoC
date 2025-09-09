package com.realestate.db

import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import java.time.Instant

case class OutboxRow(id: UUID, topic: String, key: String, payload: String, createdAt: Instant)

class OutboxTable(tag: Tag) extends Table[OutboxRow](tag, "outbox") {
  def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)

  def topic: Rep[String] = column[String]("topic")

  def key: Rep[String] = column[String]("key")

  def payload: Rep[String] = column[String]("payload")

  def createdAt: Rep[Instant] = column[Instant]("created_at")

  def * = (id, topic, key, payload, createdAt) <> (OutboxRow.tupled, OutboxRow.unapply)
}
