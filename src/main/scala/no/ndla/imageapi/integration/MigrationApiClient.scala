package no.ndla.imageapi.integration

import no.ndla.imageapi.ImageApiProperties
import no.ndla.network.NdlaClient

import scala.util.Try
import scalaj.http.Http

trait MigrationApiClient {
  this: NdlaClient =>

  val migrationApiClient: MigrationApiClient

  class MigrationApiClient {
    val imageMetadataEndpoint = s"${ImageApiProperties.MigrationHost}/images/:image_nid"

    def getMetaDataForImage(imageNid: String): Try[MainImageImport] = {
      ndlaClient.fetch[MainImageImport](
        Http(imageMetadataEndpoint.replace(":image_nid", imageNid)),
        Some(ImageApiProperties.MigrationUser), Some(ImageApiProperties.MigrationPassword))
    }
  }
}

case class MainImageImport(mainImage: ImageMeta, authors: List[ImageAuthor], license: Option[String], origin: Option[String], translations: List[ImageMeta])
case class ImageMeta(nid: String, tnid: String, language: String, title: String, alttext: String, changed: String, originalFile: String, originalMime: String, originalSize: String) {
  def isMainImage = nid == tnid || tnid == "0"
  def isTranslation = !isMainImage
}
case class ImageAuthor(typeAuthor: String, name: String)