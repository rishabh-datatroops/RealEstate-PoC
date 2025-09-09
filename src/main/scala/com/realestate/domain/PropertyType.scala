package com.realestate.domain

import spray.json._

sealed trait PropertyType {
  def name: String
  def description: String
}

object PropertyType {
  case object Residential extends PropertyType {
    val name = "RESIDENTIAL"
    val description = "Houses, apartments, condos for living"
  }
  
  case object Commercial extends PropertyType {
    val name = "COMMERCIAL"
    val description = "Office buildings, retail spaces, restaurants"
  }
  
  case object Industrial extends PropertyType {
    val name = "INDUSTRIAL"
    val description = "Factories, warehouses, manufacturing facilities"
  }
  
  case object Land extends PropertyType {
    val name = "LAND"
    val description = "Vacant land, plots, agricultural land"
  }
  
  case object MixedUse extends PropertyType {
    val name = "MIXED_USE"
    val description = "Combined residential and commercial properties"
  }
  
  case object Investment extends PropertyType {
    val name = "INVESTMENT"
    val description = "Properties for investment purposes"
  }

  val all: Set[PropertyType] = Set(Residential, Commercial, Industrial, Land, MixedUse, Investment)
  
  def fromString(name: String): Option[PropertyType] = {
    all.find(_.name == name.toUpperCase)
  }
  
  def fromStringOrThrow(name: String): PropertyType = {
    fromString(name).getOrElse(
      throw new IllegalArgumentException(s"Invalid property type: $name. Valid types: ${all.map(_.name).mkString(", ")}")
    )
  }
}

object PropertyTypeJsonProtocol extends DefaultJsonProtocol {
  implicit val propertyTypeFormat: JsonFormat[PropertyType] = new JsonFormat[PropertyType] {
    override def write(propertyType: PropertyType): JsValue = JsString(propertyType.name)
    
    override def read(value: JsValue): PropertyType = value match {
      case JsString(s) => PropertyType.fromStringOrThrow(s)
      case other => deserializationError(s"Expected PropertyType as JsString, but got: $other")
    }
  }
}

