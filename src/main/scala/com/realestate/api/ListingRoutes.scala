package com.realestate.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.realestate.api.SharedJsonProtocol._
import com.realestate.db.ListingRepository
import com.realestate.domain.ListingJsonProtocol._
import com.realestate.domain.{Listing, PropertyType}
import spray.json._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class CreateListingRequest(address: String, price: Long, propertyType: Option[String] = None)
case class UpdatePriceRequest(price: Long)
case class UpdatePropertyTypeRequest(propertyType: String)

object ApiResponseJsonProtocol extends DefaultJsonProtocol {
  implicit val createListingRequestFormat = jsonFormat3(CreateListingRequest)
  implicit val updatePriceRequestFormat = jsonFormat1(UpdatePriceRequest)
  implicit val updatePropertyTypeRequestFormat = jsonFormat1(UpdatePropertyTypeRequest)
}

class ListingRoutes(repo: ListingRepository, projector: com.realestate.consumer.SearchProjector)(implicit ec: ExecutionContext) {
  import ApiResponseJsonProtocol._

  val routes: Route =
    pathPrefix("listings") {
      concat(
        pathEndOrSingleSlash {
          concat(
            post {
              entity(as[CreateListingRequest]) { request =>
                val id = UUID.randomUUID()
                val propertyType = request.propertyType
                  .map(PropertyType.fromStringOrThrow)
                  .getOrElse(PropertyType.Residential)

                val listing = Listing(id, request.address, request.price, propertyType)

                onComplete(repo.createListing(listing)) {
                  case Success(_) =>
                    complete(
                      StatusCodes.Created,
                      ApiResponse(
                        message = "Listing created successfully",
                        data = Some(listing.toJson)
                      )
                    )
                  case Failure(ex: IllegalArgumentException) =>
                    complete(StatusCodes.BadRequest, ApiResponse(s"Invalid property type: ${ex.getMessage}"))
                  case Failure(ex) =>
                    complete(StatusCodes.InternalServerError, ApiResponse(s"Error creating listing: ${ex.getMessage}"))
                }
              }
            },
            get {
              onSuccess(repo.allListings()) { listings =>
                complete(ApiResponse(
                  message = "Listings retrieved successfully",
                  data = Some(JsArray(listings.map(_.toJson).toVector))
                ))
              }
            }
          )
        },
        path("types") {
          get {
            complete(ApiResponse(
              message = "Property types retrieved successfully",
              data = Some(JsArray(PropertyType.all.map(pt =>
                JsObject(
                  "name" -> JsString(pt.name),
                  "description" -> JsString(pt.description)
                )
              ).toVector))
            ))
          }
        },
        path("type" / Segment) { propertyTypeStr =>
          get {
            try {
              val propertyType = PropertyType.fromStringOrThrow(propertyTypeStr)
              onSuccess(repo.findListingsByPropertyType(propertyType)) { listings =>
                complete(ApiResponse(
                  message = s"Listings for ${propertyType.name} retrieved successfully",
                  data = Some(JsArray(listings.map(_.toJson).toVector))
                ))
              }
            } catch {
              case _: IllegalArgumentException =>
                complete(StatusCodes.BadRequest, ApiResponse(
                  s"Invalid property type: $propertyTypeStr. Valid types: ${PropertyType.all.map(_.name).mkString(", ")}"
                ))
            }
          }
        },
        // FIXED UUID nesting
        pathPrefix(JavaUUID) { id =>
          concat(
            // GET /listings/{id}
            pathEndOrSingleSlash {
              get {
                onSuccess(repo.getListing(id)) {
                  case Some(listing) => complete(ApiResponse(
                    message = "Listing retrieved successfully",
                    data = Some(listing.toJson)
                  ))
                  case None => complete(StatusCodes.NotFound, ApiResponse("Listing not found"))
                }
              }
            },
            // PUT /listings/{id}/price
            path("price") {
              put {
                entity(as[UpdatePriceRequest]) { request =>
                  onComplete(repo.updatePrice(id, request.price)) {
                    case Success(_) =>
                      complete(ApiResponse(s"Price updated successfully for listing $id"))
                    case Failure(_: NoSuchElementException) =>
                      complete(StatusCodes.NotFound, ApiResponse("Listing not found"))
                    case Failure(_) =>
                      complete(StatusCodes.InternalServerError, ApiResponse("Error updating price"))
                  }
                }
              }
            },
            // PUT /listings/{id}/propertyType
            path("propertyType") {
              put {
                entity(as[UpdatePropertyTypeRequest]) { request =>
                  try {
                    val propertyType = PropertyType.fromStringOrThrow(request.propertyType)
                    onComplete(repo.updatePropertyType(id, propertyType)) {
                      case Success(_) =>
                        complete(ApiResponse(s"Property type updated successfully for listing $id"))
                      case Failure(_: NoSuchElementException) =>
                        complete(StatusCodes.NotFound, ApiResponse("Listing not found"))
                      case Failure(ex) =>
                        complete(StatusCodes.InternalServerError, ApiResponse(s"Error updating property type: ${ex.getMessage}"))
                    }
                  } catch {
                    case _: IllegalArgumentException =>
                      complete(
                        StatusCodes.BadRequest,
                        ApiResponse(
                          s"Invalid property type: ${request.propertyType}. Valid types: ${PropertyType.all.map(_.name).mkString(", ")}"
                        )
                      )
                  }
                }
              }
            }
          )
        }
      )
    } ~
    pathPrefix("search") {
      concat(
        pathEndOrSingleSlash {
          get {
            parameters("q".as[String]) { q =>
              val results = projector.search(q)
              complete(ApiResponse(
                message = s"Search results for '$q'",
                data = Some(JsArray(results.map { info =>
                  JsObject(
                    "address" -> JsString(info.address),
                    "propertyType" -> JsString(info.propertyType),
                    "price" -> JsNumber(info.price)
                  )
                }.toVector))
              ))
            }
          }
        },
        path("propertyType" / Segment) { propertyType =>
          get {
            val results = projector.searchByPropertyType(propertyType)
            complete(ApiResponse(
              message = s"Search results for property type '$propertyType'",
              data = Some(JsArray(results.map { info =>
                JsObject(
                  "address" -> JsString(info.address),
                  "propertyType" -> JsString(info.propertyType),
                  "price" -> JsNumber(info.price)
                )
              }.toVector))
            ))
          }
        },
        path("price") {
          get {
            parameters("min".as[Long], "max".as[Long]) { (min, max) =>
              val results = projector.searchByPriceRange(min, max)
              complete(ApiResponse(
                message = s"Search results for price range $min - $max",
                data = Some(JsArray(results.map { info =>
                  JsObject(
                    "address" -> JsString(info.address),
                    "propertyType" -> JsString(info.propertyType),
                    "price" -> JsNumber(info.price)
                  )
                }.toVector))
              ))
            }
          }
        }
      )
    } ~
    path("health") {
      get {
        complete(ApiResponse("Service is healthy"))
      }
    }
}
