/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{AuthUser, Domains}
import no.ndla.network.secrets.PropertyKeys

import scala.util.{Failure, Success}
import scala.util.Properties._

object ImageApiProperties extends LazyLogging {
  val IsKubernetes: Boolean = propOrNone("NDLA_IS_KUBERNETES").isDefined

  val Environment: String = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "image-api"
  val Auth0LoginEndpoint = s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val RoleWithWriteAccess = "images:write"

  val ApplicationPort: Int = propOrElse("APPLICATION_PORT", "80").toInt
  val DefaultLanguage: String = propOrElse("DEFAULT_LANGUAGE", "nb")
  val ContactName: String = propOrElse("CONTACT_NAME", "NDLA")
  val ContactUrl: String = propOrElse("CONTACT_URL", "ndla.no")
  val ContactEmail: String = propOrElse("CONTACT_EMAIL", "support+api@ndla.no")
  val TermsUrl: String = propOrElse("TERMS_URL", "https://om.ndla.no/tos")

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"
  val HealthControllerPath = "/health"
  val ImageApiBasePath = "/image-api"
  val ApiDocsPath = s"$ImageApiBasePath/api-docs"
  val ImageControllerPath = s"$ImageApiBasePath/v2/images"
  val RawControllerPath = s"$ImageApiBasePath/raw"

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

  val allowedAuthors
    : Seq[String] = ImageApiProperties.creatorTypes ++ ImageApiProperties.processorTypes ++ ImageApiProperties.rightsholderTypes

  val IsoMappingCacheAgeInMs: Int = 1000 * 60 * 60 // 1 hour caching
  val LicenseMappingCacheAgeInMs: Int = 1000 * 60 * 60 // 1 hour caching

  val MaxImageFileSizeBytes: Int = 1024 * 1024 * 40 // 40 MiB
  val ImageScalingUltraMinSize: Int = 640
  val ImageScalingUltraMaxSize: Int = 2080

  val MetaMaxConnections = 10

  def MetaUserName: String = prop(PropertyKeys.MetaUserNameKey)
  def MetaPassword: String = prop(PropertyKeys.MetaPasswordKey)
  def MetaResource: String = prop(PropertyKeys.MetaResourceKey)
  def MetaServer: String = prop(PropertyKeys.MetaServerKey)
  def MetaPort: Int = prop(PropertyKeys.MetaPortKey).toInt
  def MetaSchema: String = prop(PropertyKeys.MetaSchemaKey)

  val StorageName: String = propOrElse("IMAGE_FILE_S3_BUCKET", s"$Environment.images.ndla")

  val SearchIndex: String = propOrElse("SEARCH_INDEX_NAME", "images")
  val SearchDocument = "image"
  val TagSearchIndex: String = propOrElse("TAG_SEARCH_INDEX_NAME", "tags")
  val TagSearchDocument = "tag"

  val DefaultPageSize: Int = 10
  val MaxPageSize: Int = 10000
  val IndexBulkSize = 1000
  val SearchServer: String = propOrElse("SEARCH_SERVER", "http://search-image-api.ndla-local")
  val RunWithSignedSearchRequests: Boolean = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val ElasticSearchIndexMaxResultWindow = 10000
  val ElasticSearchScrollKeepAlive = "1m"
  val InitialScrollContextKeywords = List("0", "initial", "start", "first")

  val DraftApiHost: String = propOrElse("DRAFT_API_HOST", "draft-api.ndla-local")

  lazy val Domain: String = propOrElse("BACKEND_API_DOMAIN", Domains.get(Environment))
  val ImageApiUrlBase: String = Domain + ImageControllerPath + "/"
  val RawImageUrlBase: String = Domain + RawControllerPath

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    propOrNone(key) match {
      case Some(prop) => prop
      case _          => default
    }
  }

}
