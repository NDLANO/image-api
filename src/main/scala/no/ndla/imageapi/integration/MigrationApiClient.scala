/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import no.ndla.imageapi.ImageApiProperties.{MigrationHost, MigrationUser, MigrationPassword, Environment}
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http
import com.netaporter.uri.dsl._

trait MigrationApiClient {
  this: NdlaClient =>

  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    val DBSource = if (Environment == "prod") "cm" else "red"
    val imageMetadataEndpoint = s"$MigrationHost/images/:image_nid" ? (s"db-source" -> s"$DBSource")

    def getMetaDataForImage(imageNid: String): Try[MainImageImport] = {
      ndlaClient.fetchWithBasicAuth[MainImageImport](
        Http(imageMetadataEndpoint.replace(":image_nid", imageNid)),
        MigrationUser, MigrationPassword)
    }
  }
}

case class MainImageImport(mainImage: ImageMeta, authors: List[ImageAuthor], license: Option[String], origin: Option[String], translations: List[ImageMeta])
case class ImageMeta(nid: String, tnid: String, language: String, title: String, alttext: Option[String], changed: String, originalFile: String, originalMime: String, originalSize: String, caption: Option[String]) {
  def isMainImage = nid == tnid || tnid == "0"
  def isTranslation = !isMainImage
}
case class ImageAuthor(typeAuthor: String, name: String)