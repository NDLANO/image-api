/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import io.lemonlabs.uri.{Url, UrlPath}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties.{
  ImageImportSource,
  NdlaRedPassword,
  NdlaRedUsername,
  creatorTypes,
  oldCreatorTypes,
  oldProcessorTypes,
  oldRightsholderTypes,
  processorTypes,
  redDBSource,
  rightsholderTypes
}
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.integration._
import no.ndla.imageapi.model.domain.{ImageMetaInformation, ImageTag}
import no.ndla.imageapi.model.{ImageStorageException, ImportException, Language, domain}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.ImageIndexService
import no.ndla.mapping.License.getLicense
import no.ndla.mapping.LicenseDefinition

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

trait ImportService {
  this: ImageStorageService
    with ImageRepository
    with MigrationApiClient
    with ImageIndexService
    with ConverterService
    with TagsService
    with Clock
    with User =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val redHost = s"https://red.ndla.no/sites/default/files/images"
    val cmHost = s"https://ndla.no/sites/default/files/images"

    def importImage(externalImageId: String): Try[domain.ImageMetaInformation] = {
      val start = System.currentTimeMillis()
      val importedImage = for {
        importMeta <- migrationApiClient.getMetaDataForImage(externalImageId)
        rawImageMeta <- uploadRawImage(importMeta.mainImage)
        importedImage <- persistMetadata(toDomainImage(importMeta, rawImageMeta), importMeta.mainImage.nid)
        _ <- imageIndexService.indexDocument(importedImage)
      } yield importedImage

      importedImage match {
        case Success(i) =>
          logger.info(
            s"Successfully imported image with external ID $externalImageId (${i.id.get}) in ${System.currentTimeMillis() - start} ms")
        case Failure(f) => logger.error(s"Failed to import image with externalId $externalImageId: ${f.getMessage}")
      }

      importedImage
    }

    private[service] def uploadRawImage(rawImgMeta: ImageMeta): Try[domain.Image] = {
      val image =
        domain.Image(UrlPath.parse(rawImgMeta.originalFile).toAbsolute.toString,
                     rawImgMeta.originalSize.toInt,
                     rawImgMeta.originalMime)
      val sizeMatches = imageStorage.objectSize(rawImgMeta.originalFile) == rawImgMeta.originalSize.toInt

      if (imageStorage.objectExists(rawImgMeta.originalFile) && sizeMatches) {
        Success(image)
      } else {
        val request = if (ImageImportSource == redDBSource) {
          Http(Url.parse(redHost + image.fileName).toString).auth(NdlaRedUsername, NdlaRedPassword)
        } else {
          Http(Url.parse(cmHost + image.fileName).toString)
        }

        val tryResUpload = imageStorage.uploadFromUrl(image, rawImgMeta.originalFile, request)
        tryResUpload match {
          case Failure(f) =>
            Failure(new ImageStorageException(s"Upload of image '${image.fileName}' to S3 failed.: ${f.getMessage}"))
          case Success(_) => Success(image)
        }
      }
    }

    private def getTags(nodeIds: Seq[String], langs: Seq[String]): Seq[ImageTag] = {
      nodeIds
        .flatMap(nid => tagsService.forImage(nid).getOrElse(Seq.empty))
        .groupBy(_.language)
        .map { case (lang, t) => ImageTag(t.flatMap(_.tags), lang) }
        .filter(tag => langs.contains(tag.language))
        .toSeq
    }

    private[service] def toDomainImage(imageMeta: MainImageImport,
                                       rawImage: domain.Image): domain.ImageMetaInformation = {
      val (translationTitles, translationAltTexts, translationCaptions) = toDomainTranslationFields(imageMeta)
      val mainLanguage = Option(imageMeta.mainImage.language).filter(_.nonEmpty).getOrElse(Language.UnknownLanguage)
      val titles = translationTitles :+ domain.ImageTitle(imageMeta.mainImage.title, mainLanguage)
      val altTexts = translationAltTexts ++ imageMeta.mainImage.alttext
        .map(alt => Seq(domain.ImageAltText(alt, mainLanguage)))
        .getOrElse(Seq.empty)
      val captions = translationCaptions ++ imageMeta.mainImage.caption
        .map(cap => Seq(domain.ImageCaption(cap, mainLanguage)))
        .getOrElse(Seq.empty)
      val tags = getTags(imageMeta.mainImage.nid +: imageMeta.translations.map(_.nid),
                         Language.findSupportedLanguages(titles, altTexts, captions))

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
        authUser.userOrClientid(),
        clock.now()
      )
    }

    private def persistMetadata(image: domain.ImageMetaInformation,
                                externalImageId: String): Try[ImageMetaInformation] = {
      imageRepository.withExternalId(externalImageId) match {
        case Some(dbMeta) => Try(imageRepository.update(image.copy(id = dbMeta.id), dbMeta.id.get))
        case None         => Try(imageRepository.insertWithExternalId(image, externalImageId))
      }
    }

    private[service] def oldToNewLicenseKey(license: String): Option[LicenseDefinition] = {
      val licenses = Map(
        "by" -> "CC-BY-4.0",
        "by-sa" -> "CC-BY-SA-4.0",
        "by-nc" -> "CC-BY-NC-4.0",
        "by-nd" -> "CC-BY-ND-4.0",
        "by-nc-sa" -> "CC-BY-NC-SA-4.0",
        "by-nc-nd" -> "CC-BY-NC-ND-4.0",
        "by-3.0" -> "CC-BY-4.0",
        "by-sa-3.0" -> "CC-BY-SA-4.0",
        "by-nc-3.0" -> "CC-BY-NC-4.0",
        "by-nd-3.0" -> "CC-BY-ND-4.0",
        "by-nc-sa-3.0" -> "CC-BY-NC-SA-4.0",
        "by-nc-nd-3.0" -> "CC-BY-NC-ND-4.0",
        "copyrighted" -> "COPYRIGHTED",
        "cc0" -> "CC0-1.0",
        "pd" -> "PD",
        "nolaw" -> "CC0-1.0",
        "noc" -> "PD"
      )
      val newLicense = getLicense(licenses.getOrElse(license, license))
      if (newLicense.isEmpty) {
        throw new ImportException(s"License $license is not supported.")
      }
      newLicense
    }

    private def toNewAuthorType(author: ImageAuthor): domain.Author = {
      val creatorMap = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
      val processorMap = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
      val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

      (creatorMap(author.typeAuthor.toLowerCase),
       processorMap(author.typeAuthor.toLowerCase),
       rightsholderMap(author.typeAuthor.toLowerCase)) match {
        case (t: String, _, _) => domain.Author(t.capitalize, author.name)
        case (_, t: String, _) => domain.Author(t.capitalize, author.name)
        case (_, _, t: String) => domain.Author(t.capitalize, author.name)
        case (_, _, _)         => domain.Author(author.typeAuthor, author.name)
      }
    }

    private[service] def toDomainCopyright(imageMeta: MainImageImport): domain.Copyright = {
      val domainLicense = imageMeta.license.flatMap(oldToNewLicenseKey).map(_.license.toString).getOrElse("COPYRIGHTED")

      val creators =
        imageMeta.authors.filter(a => oldCreatorTypes.contains(a.typeAuthor.toLowerCase)).map(toNewAuthorType)
      val processors =
        imageMeta.authors.filter(a => oldProcessorTypes.contains(a.typeAuthor.toLowerCase)).map(toNewAuthorType)
      val rightsholders =
        imageMeta.authors.filter(a => oldRightsholderTypes.contains(a.typeAuthor.toLowerCase)).map(toNewAuthorType)

      domain.Copyright(domainLicense,
                       imageMeta.origin.getOrElse(""),
                       creators,
                       processors,
                       rightsholders,
                       agreementId = None,
                       validFrom = None,
                       validTo = None)
    }

    private def toDomainTranslationFields(
        imageMeta: MainImageImport): (Seq[domain.ImageTitle], Seq[domain.ImageAltText], Seq[domain.ImageCaption]) = {
      imageMeta.translations
        .map(tr => {
          val language = tr.language
          (domain.ImageTitle(tr.title, language),
           domain.ImageAltText(tr.alttext.getOrElse(""), language),
           domain.ImageCaption(tr.caption.getOrElse(""), language))
        })
        .unzip3
    }

  }

}
