/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets

import scala.util.Properties._


object ImageApiProperties extends LazyLogging {
  val SecretsFile = "image_api.secrets"

  val ApplicationPort = 80
  val ContactEmail = "christergundersen@ndla.no"
  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val HealthControllerPath = "/health"
  val ImageControllerPath = "/images"

  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  val secrets = readSecrets(SecretsFile).getOrElse(throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile"))

  val MetaInitialConnections = 3
  val MetaMaxConnections = 20
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")
  val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource = prop(PropertyKeys.MetaResourceKey)
  val MetaServer = prop(PropertyKeys.MetaServerKey)
  val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = prop(PropertyKeys.MetaSchemaKey)

  val StorageName = s"$Environment.images.ndla"

  val SearchIndex = "images"
  val SearchDocument = "image"
  val DefaultPageSize: Int = 10
  val MaxPageSize: Int = 100
  val IndexBulkSize = 1000
  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-image-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean

  val MappingHost = "mapping-api.ndla-local"
  val TopicAPIUrl = prop("TOPIC_API_URL")

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val Domain = Map(
    "local" -> "http://localhost",
    "prod" -> "http://api.ndla.no"
  ).getOrElse(Environment, s"http://api.$Environment.ndla.no")
  val ImageUrlBase = Domain + ImageControllerPath + "/"


  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    secrets.get(key).flatten match {
      case Some(secret) => secret
      case None =>
        envOrNone(key) match {
          case Some(env) => env
          case None => default
        }
    }
  }
}
