/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.netaporter.uri.Uri.parse
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties.{ImageImportSource, NdlaRedPassword, NdlaRedUsername, redDBSource}
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.integration._
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.{Language, S3UploadException, domain}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexBuilderService
import no.ndla.mapping.License.getLicense
import no.ndla.mapping.ISO639.get6391CodeFor6392Code

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

trait ImportService {
  this: ImageStorageService with ImageRepository with MigrationApiClient with IndexBuilderService with ConverterService with TagsService
    with Clock with User =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val redHost = s"https://red.ndla.no/sites/default/files/images"
    val cmHost = s"https://ndla.no/sites/default/files/images"

    val ImportUserId = "content-import-client"

    def importImage(externalImageId: String): Try[domain.ImageMetaInformation] = {
      val start = System.currentTimeMillis()
      val importedImage = for {
        importMeta <- migrationApiClient.getMetaDataForImage(externalImageId)
        rawImageMeta <- uploadRawImage(importMeta.mainImage)
        importedImage <- persistMetadata(toDomainImage(importMeta, rawImageMeta), importMeta.mainImage.nid)
        _ <- indexBuilderService.indexDocument(importedImage)
      } yield importedImage

      importedImage match {
        case Success(i) => logger.info(s"Successfully imported image with external ID $externalImageId (${i.id.get}) in ${System.currentTimeMillis() - start} ms")
        case Failure(f) => logger.error(s"Failed to import image with externalId $externalImageId: ${f.getMessage}")
      }

      importedImage
    }

    private[service] def uploadRawImage(rawImgMeta: ImageMeta): Try[domain.Image] = {
      val image = domain.Image(parse(rawImgMeta.originalFile).toString, rawImgMeta.originalSize.toInt, rawImgMeta.originalMime)

      if (imageStorage.objectExists(rawImgMeta.originalFile) && imageStorage.objectSize(rawImgMeta.originalFile) == rawImgMeta.originalSize.toInt) {
        Success(image)
      } else {
        val request = if (ImageImportSource == redDBSource) {
          Http(parse(redHost + image.fileName).toString).auth(NdlaRedUsername, NdlaRedPassword)
        } else {
          Http(parse(cmHost + image.fileName).toString)
        }

        val tryResUpload = imageStorage.uploadFromUrl(image, rawImgMeta.originalFile, request)
        tryResUpload match {
          case Failure(f) => Failure(new S3UploadException(s"Upload of image '${image.fileName}' to S3 failed.: ${f.getMessage}"))
          case Success(_) => Success(image)
        }
      }
    }

    private def toDomainImage(imageMeta: MainImageImport, rawImage: domain.Image): domain.ImageMetaInformation = {
      val (translationTitles, translationAltTexts, translationCaptions) = toDomainTranslationFields(imageMeta)
      val tags = tagsService.forImage(imageMeta.mainImage.nid) match {
        case Success(t) => t
        case Failure(e) =>
          logger.warn(s"Could not import tags for node ${imageMeta.mainImage.nid}", e)
          List.empty
      }
      val mainLanguage = Option(imageMeta.mainImage.language).filter(_.nonEmpty).getOrElse(Language.UnknownLanguage)
      val titles = translationTitles :+ domain.ImageTitle(imageMeta.mainImage.title, mainLanguage)
      val altTexts = translationAltTexts ++ imageMeta.mainImage.alttext.map(alt => Seq(domain.ImageAltText(alt, mainLanguage))).getOrElse(Seq.empty)
      val captions = translationCaptions ++ imageMeta.mainImage.caption.map(cap => Seq(domain.ImageCaption(cap, mainLanguage))).getOrElse(Seq.empty)

      domain.ImageMetaInformation(
        None,
        titles,
        altTexts,
        rawImage.fileName,
        rawImage.size,
        rawImage.contentType,
        toDomainCopyright(imageMeta),
        tags,
        captions,
        ImportUserId,
        clock.now()
      )
    }

    private def persistMetadata(image: domain.ImageMetaInformation, externalImageId: String): Try[ImageMetaInformation] = {
      imageRepository.withExternalId(externalImageId) match {
        case Some(dbMeta) => Try(imageRepository.update(image.copy(id=dbMeta.id), dbMeta.id.get))
        case None => Try(imageRepository.insertWithExternalId(image, externalImageId))
      }
    }

    private def toDomainCopyright(imageMeta: MainImageImport): domain.Copyright = {
      val domainLicense = imageMeta.license.flatMap(getLicense)
        .map(license => domain.License(license.license, license.description, license.url))
        .getOrElse(domain.License(imageMeta.license.get, imageMeta.license.get, None))

      domain.Copyright(domainLicense,
        imageMeta.origin.getOrElse(""),
        imageMeta.authors.map(a => domain.Author(a.typeAuthor, a.name)))
    }

    private def toDomainTranslationFields(imageMeta: MainImageImport): (Seq[domain.ImageTitle], Seq[domain.ImageAltText], Seq[domain.ImageCaption]) = {
      imageMeta.translations.map(tr => {
        val language = tr.language
        (domain.ImageTitle(tr.title, language),
          domain.ImageAltText(tr.alttext.getOrElse(""), language),
          domain.ImageCaption(tr.caption.getOrElse(""), language))
        }).unzip3
    }

  }
}