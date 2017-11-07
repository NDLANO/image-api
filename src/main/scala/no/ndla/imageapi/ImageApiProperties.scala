/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.Domains
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets

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
  val ImageControllerPath = s"$ImageApiBasePath/v2/images"
  val RawControllerPath = s"$ImageApiBasePath/raw"

  val DefaultLanguage = "nb"

  final val oldCreatorTypes = List("opphavsmann", "fotograf", "kunstner", "redaksjonelt", "forfatter", "manusforfatter", "innleser", "oversetter", "regissør", "illustratør", "medforfatter", "komponist")
  final val creatorTypes = List("originator", "photographer", "artist", "editorial", "writer", "scriptwriter", "reader", "translator", "director", "illustrator", "cowriter", "composer")

  final val oldProcessorTypes = List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  final val processorTypes = List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  final val oldRightsholderTypes = List("rettighetshaver", "forlag", "distributør", "leverandør")
  final val rightsholderTypes = List("rightsholder", "publisher", "distributor", "supplier")
  final val authorTypeString = (ImageApiProperties.creatorTypes ++ ImageApiProperties.processorTypes ++ ImageApiProperties.rightsholderTypes).mkString(",")

  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  val MaxImageFileSizeBytes = 1024 * 1024 * 40 // 40 MiB

  val MetaInitialConnections = 3
  val MetaMaxConnections = 20
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")
  val (redDBSource, cmDBSource) = ("red", "cm")
  val CMSourceEnvironments = "prod" :: Nil
  val ImageImportSource = if (CMSourceEnvironments.contains(Environment)) cmDBSource else redDBSource

  val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  val MetaResource = prop(PropertyKeys.MetaResourceKey)
  val MetaServer = prop(PropertyKeys.MetaServerKey)
  val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  val MetaSchema = prop(PropertyKeys.MetaSchemaKey)

  val StorageName = s"$Environment.images.ndla"

  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "images")
  val SearchDocument = "image"
  val DefaultPageSize: Int = 10
  val MaxPageSize: Int = 100
  val IndexBulkSize = 1000
  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-image-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val ElasticSearchIndexMaxResultWindow = 10000

  val MappingHost = "mapping-api.ndla-local"
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val NdlaRedUsername = prop("NDLA_RED_USERNAME")
  val NdlaRedPassword = prop("NDLA_RED_PASSWORD")

  val Domain = Domains.get(Environment)
  val ImageApiUrlBase = Domain + ImageControllerPath + "/"
  val RawImageUrlBase = Domain + RawControllerPath

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
}
