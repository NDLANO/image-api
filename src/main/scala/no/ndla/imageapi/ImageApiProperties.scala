/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging

object ImageApiProperties extends LazyLogging {

  val EnvironmentFile = "/image-api.env"
  val ImageApiProps = io.Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().map(key => key -> scala.util.Properties.envOrNone(key)).toMap

  val ContactEmail = get("CONTACT_EMAIL")
  val HostAddr = get("HOST_ADDR")
  val Domains = get("DOMAINS").split(",") ++ Array(HostAddr)
  val SearchIndex = get("SEARCH_INDEX")
  val SearchDocument = get("SEARCH_DOCUMENT")
  val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  def verify() = {
    val missingProperties = ImageApiProps.filter(entry => entry._2.isEmpty).toList
    if(missingProperties.length > 0){
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
    }
  }

  def get(envKey: String): String = {
    ImageApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  def getInt(envKey: String):Integer = {
    get(envKey).toInt
  }

}
