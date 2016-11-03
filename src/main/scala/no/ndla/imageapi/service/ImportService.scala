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
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexService

import scala.util.Try

trait ImportService {
  this: ImageStorageService with ImageRepository with MigrationApiClient with IndexService with ConverterService with MappingApiClient with TagsService =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"

    def importImage(imageId: String): Try[domain.ImageMetaInformation] = {
      val imported = migrationApiClient.getMetaDataForImage(imageId).map(upload)
      imported.foreach(indexService.indexDocument)
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

      val mainTitle = domain.ImageTitle(imageMeta.mainImage.title, imageLang)
      val mainAlttext = imageMeta.mainImage.alttext.map(x => domain.ImageAltText(x, imageLang))
      val mainCaption = imageMeta.mainImage.caption.map(x => domain.ImageCaption(x, imageLang))

      val (titles, alttexts, captions) = imageMeta.translations.foldLeft((Seq(mainTitle), Seq(mainAlttext), Seq(mainCaption)))((result, translation) => {
        val (titles, alttexts, captions) = result
        val transLang = if (mappingApiClient.languageCodeSupported(translation.language)) Some(translation.language) else None

        (titles :+ domain.ImageTitle(translation.title, transLang),
          alttexts :+ translation.alttext.map(x => domain.ImageAltText(x, transLang)),
        captions :+ translation.caption.map(x => domain.ImageCaption(x, transLang)))
      })

      val persistedImageMetaInformation = imageRepository.withExternalId(imageMeta.mainImage.nid) match {
        case Some(dbMeta) => {
          val updated = imageRepository.update(domain.ImageMetaInformation(dbMeta.id, titles, alttexts.flatten, dbMeta.url, dbMeta.size, dbMeta.contentType, copyright, tags, captions.flatten), dbMeta.id.get)
          logger.info(s"Updated ID = ${updated.id}, External_ID = ${imageMeta.mainImage.nid} (${imageMeta.mainImage.title}) -- ${System.currentTimeMillis - start} ms")
          updated
        }
        case None => {
          val sourceUrlFull = DownloadUrlPrefix + imageMeta.mainImage.originalFile

          val fullKey = "full/" + imageMeta.mainImage.originalFile
          val full = domain.Image(fullKey, imageMeta.mainImage.originalSize.toInt, imageMeta.mainImage.originalMime)

          val imageMetaInformation = domain.ImageMetaInformation(None, titles, alttexts.flatten, full.url, full.size, full.contentType, copyright, tags, captions.flatten)

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