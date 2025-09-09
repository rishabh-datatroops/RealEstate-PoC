package com.realestate.db

import com.realestate.domain.NotificationSubscription
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class NotificationRepository(db: Database)(implicit ec: ExecutionContext) {
  private val subscriptions = TableQuery[SubscriptionTable]

  def addSubscription(sub: NotificationSubscription): Future[Unit] = {
    val row = SubscriptionRow(
      id = sub.id,
      userId = sub.userId,
      address = sub.address,
      price = sub.price,
      createdAt = Instant.now
    )
      db.run(subscriptions += row).map(_ => ())
  }

  def getSubscription(id: UUID): Future[Option[NotificationSubscription]] = {
    db.run(subscriptions.filter(_.id === id).result.headOption).map { rowOpt =>
      rowOpt.map { row =>
        NotificationSubscription(
          id = row.id,
          userId = row.userId,
          address = row.address,
          price = row.price
        )
      }
    }
  }

  def allSubscriptions(): Future[Seq[NotificationSubscription]] = {
    db.run(subscriptions.result).map { rows =>
      rows.map { row =>
        NotificationSubscription(
          id = row.id,
          userId = row.userId,
          address = row.address,
          price = row.price
        )
      }
    }
  }

  def findSubscriptionsByUserId(userId: String): Future[Seq[NotificationSubscription]] = {
    val query = subscriptions.filter(_.userId === userId)
    db.run(query.result).map { rows =>
      rows.map { row =>
        NotificationSubscription(
          id = row.id,
          userId = row.userId,
          address = row.address,
          price = row.price
        )
      }
    }
  }

  def deleteSubscription(id: UUID): Future[Int] = {
    db.run(subscriptions.filter(_.id === id).delete)
  }
}
