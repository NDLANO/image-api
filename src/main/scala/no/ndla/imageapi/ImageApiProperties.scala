/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.Secrets.readSecrets

import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Properties, Success, Try}


object ImageApiProperties extends LazyLogging {
  object PropertyKeys {
    val MetaUserNameKey = "META_USER_NAME"
    val MetaPasswordKey = "META_PASSWORD"
    val MetaResourceKey = "META_RESOURCE"
    val MetaServerKey = "META_SERVER"
    val MetaPortKey = "META_PORT"
    val MetaSchemaKey = "META_SCHEMA"
  }

  var ImageApiProps: mutable.Map[String, Option[String]] = mutable.HashMap()

  lazy val ApplicationPort = 80
  lazy val ContactEmail = "christergundersen@ndla.no"

  lazy val Domain = get("DOMAIN")
  lazy val ImageUrlBase = Domain + ImageControllerPath + "/"

  lazy val MetaUserName = get(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = get(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = get(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = get(PropertyKeys.MetaServerKey)
  lazy val MetaPort = getInt(PropertyKeys.MetaPortKey)
  lazy val MetaSchema = get(PropertyKeys.MetaSchemaKey)
  lazy val MetaInitialConnections = 3
  lazy val MetaMaxConnections = 20

  lazy val StorageName = get("STORAGE_NAME")
  lazy val StorageAccessKey = get("STORAGE_ACCESS_KEY")
  lazy val StorageSecretKey = get("STORAGE_SECRET_KEY")

  lazy val SearchServer = getOrElse("SEARCH_SERVER", "search-image-api.ndla-local")
  lazy val SearchRegion = getOrElse("SEARCH_REGION", "eu-central-1")
  lazy val SearchIndex = "images"
  lazy val SearchDocument = "image"
  lazy val DefaultPageSize: Int = 10
  lazy val MaxPageSize: Int = 100
  lazy val IndexBulkSize = 1000
  lazy val RunWithSignedSearchRequests = getOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  lazy val TopicAPIUrl = get("TOPIC_API_URL")
  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val HealthControllerPath = "/health"
  val ImageControllerPath = "/images"
  val MappingHost = "mapping-api.ndla-local"
  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  def setProperties(properties: Map[String, Option[String]]) = {
    Success(properties.foreach(prop => ImageApiProps.put(prop._1, prop._2)))
//    val missingProperties = properties.filter(_._2.isEmpty).keys
//    missingProperties.isEmpty match {
//      case true => Success(properties.foreach(prop => ImageApiProps.put(prop._1, prop._2)))
//      case false => Failure(new RuntimeException(s"Missing required properties: ${missingProperties.mkString(", ")}"))
//    }
  }

  private def getOrElse(envKey: String, defaultValue: String) = {
    ImageApiProps.get(envKey).flatten match {
      case Some(value) => value
      case None => defaultValue
    }
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

object PropertiesLoader extends LazyLogging {
  val EnvironmentFile = "/image-api.env"

  def readPropertyFile() = {
    Try(Source.fromInputStream(getClass.getResourceAsStream(EnvironmentFile)).getLines().map(key => key -> Properties.envOrNone(key)).toMap)
  }

  def load() = {
    val verification = for {
      file <- readPropertyFile()
      secrets <- readSecrets()
      didSetProperties <- ImageApiProperties.setProperties(file ++ secrets)
    } yield didSetProperties

    if(verification.isFailure){
      logger.error("Unable to load properties", verification.failed.get)
      System.exit(1)
    }
  }
}
