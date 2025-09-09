package com.realestate.api

import spray.json._
import spray.json.DefaultJsonProtocol._

case class ApiResponse(message: String, data: Option[JsValue] = None)

object SharedJsonProtocol extends DefaultJsonProtocol {
  implicit val apiResponseFormat = jsonFormat2(ApiResponse)
}

