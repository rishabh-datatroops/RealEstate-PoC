package com.realestate.service

import com.realestate.db.NotificationRepository
import com.realestate.events.{EventJsonProtocol, ListingCreated, ListingPriceChanged, ListingPropertyTypeChanged}
import org.apache.kafka.clients.consumer.KafkaConsumer
import spray.json._

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime}
import java.util.{Collections, Properties}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

case class NotificationAlert(
  userId: String,
  message: String,
  listingId: String,
  timestamp: LocalDateTime,
  notificationType: String
)

class NotificationService(repo: NotificationRepository)(implicit ec: ExecutionContext) {
  import EventJsonProtocol._

  private val recentNotifications = ListBuffer[NotificationAlert]()
  private val maxNotifications = 50 // Keep last 50 notifications
  
  def getRecentNotifications(): List[NotificationAlert] = {
    recentNotifications.toList.reverse // Most recent first
  }
  
  private def addNotification(alert: NotificationAlert): Unit = {
    recentNotifications.synchronized {
      recentNotifications += alert
      if (recentNotifications.size > maxNotifications) {
        recentNotifications.remove(0)
      }
    }
  }
  
  private def logNotification(alert: NotificationAlert): Unit = {
    val timestamp = alert.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    val separator = "=" * 80
    val notification = s"""
$separator
🚨 REAL ESTATE NOTIFICATION ALERT 🚨
$separator
Time: $timestamp
Type: ${alert.notificationType}
User: ${alert.userId}
Listing ID: ${alert.listingId}

${alert.message}
$separator
"""

    println(s"[NOTIFICATION] $notification")

    addNotification(alert)
  }

  def startConsumer(): Unit = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", "notification-service")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("auto.offset.reset", "earliest")

    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(Collections.singletonList("listings.events"))

    new Thread(() => {
      while (true) {
        try {
          val records = consumer.poll(Duration.ofMillis(500))
          for (record <- records.asScala) {
            try {
              val eventJson = record.value().parseJson
              // Try to parse as ListingCreated first
              try {
                val event = eventJson.convertTo[ListingCreated]
                checkSubscriptionsForNewListing(event)
              } catch {
                case _: spray.json.DeserializationException =>
                  // Try to parse as ListingPriceChanged
                  try {
                    val event = eventJson.convertTo[ListingPriceChanged]
                    checkSubscriptionsForPriceChange(event)
                  } catch {
                    case _: spray.json.DeserializationException =>
                      // Try to parse as ListingPropertyTypeChanged
                      try {
                        val event = eventJson.convertTo[ListingPropertyTypeChanged]
                        checkSubscriptionsForPropertyTypeChange(event)
                      } catch {
                        case ex: spray.json.DeserializationException =>
                          println(s"[NotificationService] Could not parse event: ${ex.getMessage}")
                      }
                  }
              }
            } catch {
              case ex: Exception =>
                println(s"[NotificationService] Error processing record: ${ex.getMessage}")
            }
          }
        } catch {
          case ex: Exception =>
            println(s"[NotificationService] Error polling messages: ${ex.getMessage}")
        }
      }
    }).start()
  }

  private def checkSubscriptionsForNewListing(event: ListingCreated): Unit = {
    // Get all subscriptions and check for matches
    repo.allSubscriptions().foreach { allSubscriptions =>
      allSubscriptions.foreach { sub =>
        // Check if the listing address contains the subscription address (partial match)
        // and if the listing price is within the user's budget
        if (event.address.toLowerCase.contains(sub.address.toLowerCase) && event.price <= sub.price) {
          val message = s"""
🔔 NEW LISTING MATCH FOUND!
📍 Address: ${event.address}
💰 Price: $$${event.price} (within your budget of $$${sub.price})
🏠 Property Type: ${event.propertyType.name}
🎯 Match Criteria: Address contains '${sub.address}' and price ≤ $$${sub.price}
"""
          
          val alert = NotificationAlert(
            userId = sub.userId,
            message = message,
            listingId = event.id.toString,
            timestamp = LocalDateTime.now(),
            notificationType = "NEW_LISTING_MATCH"
          )
          
          logNotification(alert)
        }
      }
    }
  }

  private def checkSubscriptionsForPriceChange(event: ListingPriceChanged): Unit = {
    // Check if the new price now matches any subscriptions
    repo.allSubscriptions().foreach { allSubscriptions =>
      allSubscriptions.foreach { sub =>
        // If the new price is now within budget, notify the user
        if (event.newPrice <= sub.price && event.oldPrice > sub.price) {
          val message = s"""
💰 PRICE DROP ALERT!
🆔 Listing ID: ${event.id}
📉 Old Price: $$${event.oldPrice}
📈 New Price: $$${event.newPrice}
💵 Savings: $$${(event.oldPrice - event.newPrice)}
🎯 Now within your budget of $$${sub.price}!
"""
          
          val alert = NotificationAlert(
            userId = sub.userId,
            message = message,
            listingId = event.id.toString,
            timestamp = LocalDateTime.now(),
            notificationType = "PRICE_DROP_ALERT"
          )
          
          logNotification(alert)
        } else if (event.newPrice <= sub.price) {
          // Price changed but was already within budget
          val priceChange = if (event.newPrice > event.oldPrice) "increased" else "decreased"
          val message = s"""
📊 PRICE UPDATE NOTIFICATION
🆔 Listing ID: ${event.id}
📉 Old Price: $$${event.oldPrice}
📈 New Price: $$${event.newPrice}
📋 Price $priceChange by $$${Math.abs(event.newPrice - event.oldPrice)}
✅ Still within your budget of $$${sub.price}
"""
          
          val alert = NotificationAlert(
            userId = sub.userId,
            message = message,
            listingId = event.id.toString,
            timestamp = LocalDateTime.now(),
            notificationType = "PRICE_UPDATE"
          )
          
          logNotification(alert)
        }
      }
    }
  }

  private def checkSubscriptionsForPropertyTypeChange(event: ListingPropertyTypeChanged): Unit = {
    println(s"[NotificationService] Property type changed for listing ${event.id}: ${event.oldPropertyType.name} -> ${event.newPropertyType.name}")

    val message = s"""
🏠 PROPERTY TYPE CHANGED
🆔 Listing ID: ${event.id}
📋 Old Type: ${event.oldPropertyType.name}
🔄 New Type: ${event.newPropertyType.name}
⚠️  Property classification updated
"""
    
    val alert = NotificationAlert(
      userId = "SYSTEM",
      message = message,
      listingId = event.id.toString,
      timestamp = LocalDateTime.now(),
      notificationType = "PROPERTY_TYPE_CHANGE"
    )
    
    logNotification(alert)
  }
}


