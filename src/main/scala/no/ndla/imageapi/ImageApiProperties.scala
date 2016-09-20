/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.io.Source

object ImageApiProperties extends LazyLogging {

  var ImageApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  lazy val ApplicationPort = getInt("APPLICATION_PORT")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"


  lazy val ContactEmail = get("CONTACT_EMAIL")
  lazy val HostAddr = get("HOST_ADDR")
  lazy val Domain = get("DOMAIN")
  val HealthControllerPath = "/health"
  val ImageControllerPath = "/images"
  lazy val ImageUrlBase = Domain + ImageControllerPath + "/"

  lazy val MetaUserName = get("META_USER_NAME")
  lazy val MetaPassword = get("META_PASSWORD")
  lazy val MetaResource = get("META_RESOURCE")
  lazy val MetaServer = get("META_SERVER")
  lazy val MetaPort = getInt("META_PORT")
  lazy val MetaInitialConnections = getInt("META_INITIAL_CONNECTIONS")
  lazy val MetaMaxConnections = getInt("META_MAX_CONNECTIONS")
  lazy val MetaSchema = get("META_SCHEMA")

  lazy val StorageName = get("STORAGE_NAME")
  lazy val StorageAccessKey = get("STORAGE_ACCESS_KEY")
  lazy val StorageSecretKey = get("STORAGE_SECRET_KEY")

  lazy val TopicAPIUrl = get("TOPIC_API_URL")

  val SearchHost = "search-engine"
  lazy val SearchServer = get("SEARCH_SERVER")
  lazy val SearchRegion = get("SEARCH_REGION")
  lazy val SearchIndex = get("SEARCH_INDEX")
  lazy val SearchDocument = get("SEARCH_DOCUMENT")
  lazy val RunWithSignedSearchRequests = getBoolean("RUN_WITH_SIGNED_SEARCH_REQUESTS")
  lazy val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  lazy val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  lazy val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  val MappingHost = "mapping-api"
  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  def verify() = {
    val missingProperties = ImageApiProps.filter(entry => entry._2.isEmpty).toList
    if (missingProperties.nonEmpty){
      missingProperties.foreach(entry => logger.error("Missing required environment variable {}", entry._1))

      logger.error("Shutting down.")
      System.exit(1)
    }
  }

  def setProperties(properties: Map[String, Option[String]]) = {
    properties.foreach(prop => ImageApiProps.put(prop._1, prop._2))
  }

  private def get(envKey: String): String = {
    ImageApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => throw new NoSuchFieldError(s"Missing environment variable $envKey")
    }
  }

  private def getInt(envKey: String):Integer = {
    get(envKey).toInt
  }

  private def getBoolean(envKey: String): Boolean = {
    get(envKey).toBoolean
  }

}

object PropertiesLoader {
  val EnvironmentFile = "/image-api.env"

  def readPropertyFile(): Map[String,Option[String]] = {
    Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().map(key => key -> scala.util.Properties.envOrNone(key)).toMap
  }

  def load() = {
    ImageApiProperties.setProperties(readPropertyFile())
    ImageApiProperties.verify()
  }
}
