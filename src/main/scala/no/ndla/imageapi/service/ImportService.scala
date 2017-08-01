/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.integration._
import no.ndla.imageapi.model.{S3UploadException, domain}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexBuilderService
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense

import scala.util.{Failure, Success, Try}

trait ImportService {
  this: ImageStorageService with ImageRepository with MigrationApiClient with IndexBuilderService with ConverterService with TagsService
    with Clock with User =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"

    def importImage(imageId: String): Try[domain.ImageMetaInformation] = {
      val imported = migrationApiClient.getMetaDataForImage(imageId).map(upload)

      imported match {
        case Success(imp) => {
          imp.foreach(indexBuilderService.indexDocument)
          imp
        }
        case Failure(f) => Failure(f)
      }
    }

    def upload(imageMeta: MainImageImport): Try[domain.ImageMetaInformation] = {
      val start = System.currentTimeMillis

      val tags = tagsService.forImage(imageMeta.mainImage.nid) match {
        case Failure(e) =>
          logger.warn(s"Could not import tags for node ${imageMeta.mainImage.nid}", e)
          List()
        case Success(tags) => tags
      }

      val authors = imageMeta.authors.map(ia => domain.Author(ia.typeAuthor, ia.name))

      val license = imageMeta.license.flatMap(l => getLicense(l)) match {
        case Some(l) => domain.License(l.license, l.description, l.url)
        case None => domain.License(imageMeta.license.get, imageMeta.license.get, None)
      }

      val copyright = domain.Copyright(license, imageMeta.origin.getOrElse(""), authors)

      val imageLang = languageCodeSupported(imageMeta.mainImage.language) match {
        case true => Some(imageMeta.mainImage.language)
        case false => None
      }

      val mainTitle = domain.ImageTitle(imageMeta.mainImage.title, imageLang)
      val mainAlttext = imageMeta.mainImage.alttext.map(x => domain.ImageAltText(x, imageLang))
      val mainCaption = imageMeta.mainImage.caption.map(x => domain.ImageCaption(x, imageLang))

      val (titles, alttexts, captions) = imageMeta.translations.foldLeft((Seq(mainTitle), Seq(mainAlttext), Seq(mainCaption)))((result, translation) => {
        val (titles, alttexts, captions) = result
        val transLang = if (languageCodeSupported(translation.language)) Some(translation.language) else None

        (titles :+ domain.ImageTitle(translation.title, transLang),
          alttexts :+ translation.alttext.map(x => domain.ImageAltText(x, transLang)),
          captions :+ translation.caption.map(x => domain.ImageCaption(x, transLang)))
      })

      val sourceUrlFull = DownloadUrlPrefix + imageMeta.mainImage.originalFile
      val key = imageMeta.mainImage.originalFile
      val image = domain.Image(key, imageMeta.mainImage.originalSize.toInt, imageMeta.mainImage.originalMime)

      if (!imageStorage.objectExists(key) || imageStorage.objectSize(key) != image.size) {
        val tryResUpload = imageStorage.uploadFromUrl(image, key, sourceUrlFull)
        tryResUpload match {
          case Failure(f) => throw new S3UploadException(s"Upload of image :[$key] to S3 failed.")
          case Success(s) =>
        }
      }

      val now = clock.now()
      val userId = "content-import-client"

      val persistedImageMetaInformation = imageRepository.withExternalId(imageMeta.mainImage.nid) match {
        case Some(dbMeta) => {
          val updated = imageRepository.update(domain.ImageMetaInformation(dbMeta.id, titles, alttexts.flatten, dbMeta.imageUrl,
            dbMeta.size, dbMeta.contentType, copyright, tags, captions.flatten, userId, now), dbMeta.id.get)
          logger.info(s"Updated ID = ${updated.id}, External_ID = ${imageMeta.mainImage.nid} (${imageMeta.mainImage.title}) -- ${System.currentTimeMillis - start} ms")
          updated
        }
        case None => {
          val imageMetaInformation = domain.ImageMetaInformation(None, titles, alttexts.flatten, image.fileName,
            image.size, image.contentType, copyright, tags, captions.flatten, userId, now)
          val inserted = imageRepository.insertWithExternalId(imageMetaInformation, imageMeta.mainImage.nid)
          logger.info(s"Inserted ID = ${inserted.id}, External_ID = ${imageMeta.mainImage.nid} (${imageMeta.mainImage.title}) -- ${System.currentTimeMillis - start} ms")
          inserted
        }
      }

      Success(persistedImageMetaInformation)
    }

    private def languageCodeSupported(languageCode: String) =
      get6391CodeFor6392CodeMappings.exists(_._1 == language)
  }

}