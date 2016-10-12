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

  lazy val ApplicationPort = getInt("APPLICATION_PORT")
  lazy val ContactEmail = get("CONTACT_EMAIL")
  lazy val HostAddr = get("HOST_ADDR")
  lazy val Domain = get("DOMAIN")
  lazy val ImageUrlBase = Domain + ImageControllerPath + "/"

  lazy val MetaUserName = get(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = get(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = get(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = get(PropertyKeys.MetaServerKey)
  lazy val MetaPort = getInt(PropertyKeys.MetaPortKey)
  lazy val MetaSchema = get(PropertyKeys.MetaSchemaKey)
  lazy val MetaInitialConnections = getInt("META_INITIAL_CONNECTIONS")
  lazy val MetaMaxConnections = getInt("META_MAX_CONNECTIONS")

  lazy val StorageName = get("STORAGE_NAME")
  lazy val StorageAccessKey = get("STORAGE_ACCESS_KEY")
  lazy val StorageSecretKey = get("STORAGE_SECRET_KEY")

  lazy val SearchServer = get("SEARCH_SERVER")
  lazy val SearchRegion = get("SEARCH_REGION")
  lazy val SearchIndex = get("SEARCH_INDEX")
  lazy val SearchDocument = get("SEARCH_DOCUMENT")
  lazy val RunWithSignedSearchRequests = getBoolean("RUN_WITH_SIGNED_SEARCH_REQUESTS")
  lazy val DefaultPageSize: Int = getInt("SEARCH_DEFAULT_PAGE_SIZE")
  lazy val MaxPageSize: Int = getInt("SEARCH_MAX_PAGE_SIZE")
  lazy val IndexBulkSize = getInt("INDEX_BULK_SIZE")

  lazy val TopicAPIUrl = get("TOPIC_API_URL")
  lazy val MigrationHost = get("MIGRATION_HOST")
  lazy val MigrationUser = get("MIGRATION_USER")
  lazy val MigrationPassword = get("MIGRATION_PASSWORD")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val HealthControllerPath = "/health"
  val ImageControllerPath = "/images"
  val MappingHost = "mapping-api"
  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  def setProperties(properties: Map[String, Option[String]]) = {
    val missingProperties = properties.filter(_._2.isEmpty).keys
    missingProperties.isEmpty match {
      case true => Success(properties.foreach(prop => ImageApiProps.put(prop._1, prop._2)))
      case false => Failure(new RuntimeException(s"Missing required properties: ${missingProperties.mkString(", ")}"))
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
