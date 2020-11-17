/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{AuthUser, Domains}
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets

import scala.util.{Failure, Success}
import scala.util.Properties._

object ImageApiProperties extends LazyLogging {
  val IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "image-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val RoleWithWriteAccess = "images:write"

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "support+api@ndla.no"
  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val HealthControllerPath = "/health"
  val ImageApiBasePath = "/image-api"
  val ApiDocsPath = s"$ImageApiBasePath/api-docs"
  val ImageControllerPath = s"$ImageApiBasePath/v2/images"
  val RawControllerPath = s"$ImageApiBasePath/raw"

  val DefaultLanguage = "nb"

  val ValidFileExtensions = Seq(".jpg", ".png", ".jpeg", ".bmp", ".gif", ".svg")

  val ValidMimeTypes = Seq(
    "image/bmp",
    "image/gif",
    "image/jpeg",
    "image/x-citrix-jpeg",
    "image/pjpeg",
    "image/png",
    "image/x-citrix-png",
    "image/x-png",
    "image/svg+xml"
  )

  val oldCreatorTypes = List("opphavsmann",
                             "fotograf",
                             "kunstner",
                             "forfatter",
                             "manusforfatter",
                             "innleser",
                             "oversetter",
                             "regissør",
                             "illustratør",
                             "medforfatter",
                             "komponist")

  val creatorTypes = List("originator",
                          "photographer",
                          "artist",
                          "writer",
                          "scriptwriter",
                          "reader",
                          "translator",
                          "director",
                          "illustrator",
                          "cowriter",
                          "composer")

  val oldProcessorTypes =
    List("bearbeider", "tilrettelegger", "redaksjonelt", "språklig", "ide", "sammenstiller", "korrektur")
  val processorTypes = List("processor", "facilitator", "editorial", "linguistic", "idea", "compiler", "correction")

  val oldRightsholderTypes = List("rettighetshaver", "forlag", "distributør", "leverandør")
  val rightsholderTypes = List("rightsholder", "publisher", "distributor", "supplier")
  val allowedAuthors = ImageApiProperties.creatorTypes ++ ImageApiProperties.processorTypes ++ ImageApiProperties.rightsholderTypes

  val IsoMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs = 1000 * 60 * 60 // 1 hour caching

  val MaxImageFileSizeBytes = 1024 * 1024 * 40 // 40 MiB

  val MetaMaxConnections = 10
  val (redDBSource, cmDBSource) = ("red", "cm")
  val ImageImportSource = redDBSource

  def MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource = prop(PropertyKeys.MetaResourceKey)
  def MetaServer = prop(PropertyKeys.MetaServerKey)
  def MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema = prop(PropertyKeys.MetaSchemaKey)

  val StorageName: String = propOrElse("IMAGE_FILE_S3_BUCKET", s"$Environment.images.ndla")

  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "images")
  val SearchDocument = "image"
  val DefaultPageSize: Int = 10
  val MaxPageSize: Int = 10000
  val IndexBulkSize = 1000
  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-image-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val ElasticSearchIndexMaxResultWindow = 10000
  val ElasticSearchScrollKeepAlive = "1m"
  val InitialScrollContextKeywords = List("0", "initial", "start", "first")

  val DraftApiHost = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")
  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  val MigrationHost = prop("MIGRATION_HOST")
  val MigrationUser = prop("MIGRATION_USER")
  val MigrationPassword = prop("MIGRATION_PASSWORD")

  val NdlaRedUsername = prop("NDLA_RED_USERNAME")
  val NdlaRedPassword = prop("NDLA_RED_PASSWORD")

  val Domain = Domains.get(Environment)
  val ImageApiUrlBase = Domain + ImageControllerPath + "/"
  val RawImageUrlBase = Domain + RawControllerPath

  lazy val secrets = {
    val SecretsFile = "image-api.secrets"
    readSecrets(SecretsFile) match {
      case Success(values) => values
      case Failure(exception) =>
        throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
    }
  }

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    propOrNone(key) match {
      case Some(prop)            => prop
      case None if !IsKubernetes => secrets.get(key).flatten.getOrElse(default)
      case _                     => default
    }
  }

}
