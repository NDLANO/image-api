package no.ndla.imageapi.service

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration._
import no.ndla.imageapi.model._
import no.ndla.imageapi.repository.ImageRepositoryComponent
import no.ndla.mapping.{ISO639Mapping, LicenseMapping}

import scala.util.{Success, Try}

trait ImportServiceComponent {
  this: ImageStorageService with ImageRepositoryComponent with MigrationApiClient with ElasticContentIndexComponent =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"
    val ThumbUrlPrefix = "http://ndla.no/sites/default/files/imagecache/fag_preset/images/"

    def importImage(imageId: String): Try[ImageMetaInformation] = {
      for {
        metadata <- migrationApiClient.getMetaDataForImage(imageId)
        converted <- Try(upload(metadata))
        indexed <- elasticContentIndex.indexDocument(converted)
      } yield indexed
    }

    def upload(imageMeta: MainImageImport): ImageMetaInformation = {
      val start = System.currentTimeMillis

      val tags = Tags.forImage(imageMeta.mainImage.nid)
      val authors = imageMeta.authors.map(ia => Author(ia.typeAuthor, ia.name))

      val license = imageMeta.license.flatMap(l => LicenseMapping.getLicenseDefinition(l)) match {
        case Some(l) => License(imageMeta.license.get, l.description, l.url)
        case None => License(imageMeta.license.get, imageMeta.license.get, None)
      }

      val copyright = Copyright(license, imageMeta.origin.getOrElse(""), authors)

      val imageLang = ISO639Mapping.languageCodeSupported(imageMeta.mainImage.language) match {
        case true => Some(imageMeta.mainImage.language)
        case false => None
      }

      var titles = List(ImageTitle(imageMeta.mainImage.title, imageLang))
      var alttexts = List(ImageAltText(imageMeta.mainImage.alttext, imageLang))
      imageMeta.translations.foreach(translation => {
        val transLang = if (ISO639Mapping.languageCodeSupported(translation.language)) Some(translation.language) else None

        titles = ImageTitle(translation.title, transLang) :: titles
        alttexts = ImageAltText(translation.alttext, transLang) :: alttexts
      })

      val persistedImageMetaInformation = imageRepository.withExternalId(imageMeta.mainImage.nid) match {
        case Some(dbMeta) => {
          val updated = imageRepository.update(ImageMetaInformation(dbMeta.id, titles, alttexts, dbMeta.images, copyright, tags), dbMeta.id)
          logger.info(s"Updated ID = ${updated.id}, External_ID = ${imageMeta.mainImage.nid} (${imageMeta.mainImage.title}) -- ${System.currentTimeMillis - start} ms")
          updated
        }
        case None => {
          val sourceUrlFull = DownloadUrlPrefix + imageMeta.mainImage.originalFile
          val sourceUrlThumb = ThumbUrlPrefix + imageMeta.mainImage.originalFile

          val imageStream = new URL(sourceUrlThumb).openStream()
          val buffer = Stream.continually(imageStream.read).takeWhile(_ != -1).map(_.toByte).toArray

          val thumbKey = "thumbs/" + imageMeta.mainImage.originalFile
          val thumb = Image(thumbKey, buffer.size, imageMeta.mainImage.originalMime)

          val fullKey = "full/" + imageMeta.mainImage.originalFile
          val full = Image(fullKey, imageMeta.mainImage.originalSize.toInt, imageMeta.mainImage.originalMime)

          val imageMetaInformation = ImageMetaInformation("0", titles, alttexts, ImageVariants(Option(thumb), Option(full)), copyright, tags)

          if (!imageStorage.contains(thumbKey)) imageStorage.uploadFromByteArray(thumb, thumbKey, buffer)
          if (!imageStorage.contains(fullKey)) imageStorage.uploadFromUrl(full, fullKey, sourceUrlFull)

          val inserted = imageRepository.insert(imageMetaInformation, imageMeta.mainImage.nid)
          logger.info(s"Inserted ID = ${inserted.id}, External_ID = ${imageMeta.mainImage.nid} (${imageMeta.mainImage.title}) -- ${System.currentTimeMillis - start} ms")
          inserted
        }
      }
      persistedImageMetaInformation
    }
  }
}