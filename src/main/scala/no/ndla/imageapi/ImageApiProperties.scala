/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.Domains
import io.digitallibrary.network.secrets.PropertyKeys
import io.digitallibrary.network.secrets.Secrets.readSecrets

import scala.util.{Failure, Success}
import scala.util.Properties._


object ImageApiProperties extends LazyLogging {
  val SecretsFile = "image-api.secrets"

  val RoleWithWriteAccess = "images:write"

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "christergundersen@ndla.no"
  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val HealthControllerPath = "/health"
  val ImageApiBasePath = "/image-api"
  val ApiDocsPath = s"$ImageApiBasePath/api-docs"
  val ImageControllerPath = s"$ImageApiBasePath/v1/images"
  val RawControllerPath = s"$ImageApiBasePath/raw"

  val DefaultLanguage = "nb"

  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  val MaxImageFileSizeBytes = 1024 * 1024 * 40 // 40 MiB

  val MetaInitialConnections = 3
  val MetaMaxConnections = 20
  val Environment = propOrElse("GDL_ENVIRONMENT", "local")
  val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource = prop(PropertyKeys.MetaResourceKey)
  val MetaServer = prop(PropertyKeys.MetaServerKey)
  val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = prop(PropertyKeys.MetaSchemaKey)

  val StorageName = s"$Environment.images.gdl"
  val CloudFrontUrl = getCloudFrontUrl(Environment)

  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "images")
  val SearchDocument = "image"
  val DefaultPageSize: Int = 10
  val MaxPageSize: Int = 100
  val IndexBulkSize = 1000
  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-image-api.gdl-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val ElasticSearchIndexMaxResultWindow = 10000

  val MappingHost = "mapping-api.ndla-local"
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  lazy val Domain = Domains.get(Environment)
  val ImageApiUrlBase = Domain + ImageControllerPath + "/"

  lazy val secrets = readSecrets(SecretsFile) match {
     case Success(values) => values
     case Failure(exception) => throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
   }

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

  def getCloudFrontUrl(env: String): String = {
    env match {
      case "prod" => "https://images.digitallibrary.io"
      case "staging" => "https://images.staging.digitallibrary.io"
      case "test" => "https://images.test.digitallibrary.io"
      case "local" => Domain + RawControllerPath
      case _ => throw new IllegalArgumentException(s"$env is not a valid env")
    }
  }
}
