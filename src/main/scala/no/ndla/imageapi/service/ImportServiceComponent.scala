/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration._
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.repository.ImageRepositoryComponent

import scala.util.Try

trait ImportServiceComponent {
  this: ImageStorageService with ImageRepositoryComponent with MigrationApiClient with ElasticContentIndexComponent with ConverterService with MappingApiClient with TagsService =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"
    val ThumbUrlPrefix = "http://ndla.no/sites/default/files/imagecache/fag_preset/images/"

    def importImage(imageId: String): Try[domain.ImageMetaInformation] = {
      val imported = migrationApiClient.getMetaDataForImage(imageId).map(upload)
      imported.foreach(elasticContentIndex.indexDocument)
      imported
    }

    def upload(imageMeta: MainImageImport): domain.ImageMetaInformation = {
      val start = System.currentTimeMillis

      val tags = tagsService.forImage(imageMeta.mainImage.nid)
      val authors = imageMeta.authors.map(ia => domain.Author(ia.typeAuthor, ia.name))

      val license = imageMeta.license.flatMap(l => mappingApiClient.getLicenseDefinition(l)) match {
        case Some(l) => l
        case None => domain.License(imageMeta.license.get, imageMeta.license.get, None)
      }

      val copyright = domain.Copyright(license, imageMeta.origin.getOrElse(""), authors)

      val imageLang = mappingApiClient.languageCodeSupported(imageMeta.mainImage.language) match {
        case true => Some(imageMeta.mainImage.language)
        case false => None
      }

      var titles = List(domain.ImageTitle(imageMeta.mainImage.title, imageLang))
      var alttexts = List(domain.ImageAltText(imageMeta.mainImage.alttext, imageLang))
      imageMeta.translations.foreach(translation => {
        val transLang = if (mappingApiClient.languageCodeSupported(translation.language)) Some(translation.language) else None

        titles = domain.ImageTitle(translation.title, transLang) :: titles
        alttexts = domain.ImageAltText(translation.alttext, transLang) :: alttexts
      })

      val persistedImageMetaInformation = imageRepository.withExternalId(imageMeta.mainImage.nid) match {
        case Some(dbMeta) => {
          val updated = imageRepository.update(domain.ImageMetaInformation(dbMeta.id, titles, alttexts, dbMeta.images, copyright, tags), dbMeta.id.get)
          logger.info(s"Updated ID = ${updated.id}, External_ID = ${imageMeta.mainImage.nid} (${imageMeta.mainImage.title}) -- ${System.currentTimeMillis - start} ms")
          updated
        }
        case None => {
          val sourceUrlFull = DownloadUrlPrefix + imageMeta.mainImage.originalFile
          val sourceUrlThumb = ThumbUrlPrefix + imageMeta.mainImage.originalFile

          val imageStream = new URL(sourceUrlThumb).openStream()
          val buffer = Stream.continually(imageStream.read).takeWhile(_ != -1).map(_.toByte).toArray

          val thumbKey = "thumbs/" + imageMeta.mainImage.originalFile
          val thumb = domain.Image(thumbKey, buffer.size, imageMeta.mainImage.originalMime)

          val fullKey = "full/" + imageMeta.mainImage.originalFile
          val full = domain.Image(fullKey, imageMeta.mainImage.originalSize.toInt, imageMeta.mainImage.originalMime)

          val imageMetaInformation = domain.ImageMetaInformation(None, titles, alttexts, domain.ImageVariants(Option(thumb), Option(full)), copyright, tags)

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