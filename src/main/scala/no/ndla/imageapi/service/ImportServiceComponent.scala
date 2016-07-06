package no.ndla.imageapi.service

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration._
import no.ndla.imageapi.model._
import no.ndla.imageapi.repository.ImageRepositoryComponent
import no.ndla.mapping.{ISO639Mapping, LicenseMapping}

trait ImportServiceComponent {
  this: ImageStorageService with ImageRepositoryComponent with MigrationApiClient =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"
    val ThumbUrlPrefix = "http://ndla.no/sites/default/files/imagecache/fag_preset/images/"

    def importImage(imageId: String): Option[String] = {
      val meta = migrationApiClient.getMetaDataForImage(imageId)

      val author = meta.authors
      val license = meta.license
      val origin = meta.origin

      val translations = meta.translations

      upload(meta) match {
        case Some(errorMsg) => Some(errorMsg) // Fail
        case _ => None // Success
      }
    }

    def upload(imageMeta: MainImageImport): Option[String] = {
      val start = System.currentTimeMillis
      try {
        val tags = Tags.forImage(imageMeta.mainImage.nid)



        val authors = imageMeta.authors.map(ia => Author(ia.typeAuthor, ia.name))

        val license = imageMeta.license.flatMap(l => LicenseMapping.getLicenseDefinition(l)) match {
          case Some((description, url)) => License(imageMeta.license.get, description, Some(url))
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
          val transLang = if(ISO639Mapping.languageCodeSupported(translation.language)) Some(translation.language) else None

          titles = ImageTitle(translation.title, transLang) :: titles
          alttexts = ImageAltText(translation.alttext, transLang) :: alttexts
        })

        imageRepository.withExternalId(imageMeta.mainImage.nid) match {
          case Some(dbMeta) => {
            imageRepository.update(ImageMetaInformation(dbMeta.id, titles, alttexts, dbMeta.images, copyright, tags), dbMeta.id)
            logger.info(s"updated {} ({}) -- ${System.currentTimeMillis - start} ms", imageMeta.mainImage.nid, imageMeta.mainImage.nid)
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

            imageRepository.insert(imageMetaInformation, imageMeta.mainImage.nid)
            logger.info(s"inserted {} ({}, {}) -- ${System.currentTimeMillis - start} ms", imageMeta.mainImage.nid, imageMeta.mainImage.title, sourceUrlFull)
          }
        }
        None
      } catch {
        case e: Exception => {
          e.printStackTrace()
          val errMsg = s"Import of node ${imageMeta.mainImage.nid} failed after ${System.currentTimeMillis - start} ms with error: ${e.getMessage}"
          logger.info(errMsg)
          Some(errMsg)
        }
      }
    }
  }
}