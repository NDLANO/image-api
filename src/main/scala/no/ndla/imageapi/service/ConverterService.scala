/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.model.Language._
import no.ndla.imageapi.model.{api, domain}
import no.ndla.network.ApplicationUrl
import io.lemonlabs.uri.{Uri, Url, UrlPath}
import io.lemonlabs.uri.dsl._
import no.ndla.imageapi.ImageApiProperties.DefaultLanguage
import no.ndla.imageapi.integration.DraftApiClient
import no.ndla.imageapi.model.domain.ModelReleasedStatus
import no.ndla.mapping.License.getLicense

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: User with Role with Clock with DraftApiClient =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {

    def asApiAuthor(domainAuthor: domain.Author): api.Author = {
      api.Author(domainAuthor.`type`, domainAuthor.name)
    }

    def asApiCopyright(domainCopyright: domain.Copyright): api.Copyright = {
      api.Copyright(
        asApiLicense(domainCopyright.license),
        domainCopyright.origin,
        domainCopyright.creators.map(asApiAuthor),
        domainCopyright.processors.map(asApiAuthor),
        domainCopyright.rightsholders.map(asApiAuthor),
        domainCopyright.agreementId,
        domainCopyright.validFrom,
        domainCopyright.validTo
      )
    }

    def asApiImage(domainImage: domain.Image, baseUrl: Option[String] = None): api.Image = {
      api.Image(baseUrl.getOrElse("") + domainImage.fileName, domainImage.size, domainImage.contentType)
    }

    def asApiImageAltText(domainImageAltText: domain.ImageAltText): api.ImageAltText = {
      api.ImageAltText(domainImageAltText.alttext, domainImageAltText.language)
    }

    def asApiImageMetaInformationWithApplicationUrlV2(domainImageMetaInformation: domain.ImageMetaInformation,
                                                      language: Option[String]): api.ImageMetaInformationV2 = {
      val baseUrl = ApplicationUrl.get
      val rawPath = baseUrl.replace("/v2/images/", "/raw")
      asImageMetaInformationV2(domainImageMetaInformation, language, ApplicationUrl.get, Some(rawPath))
    }

    def asApiImageMetaInformationWithDomainUrlV2(domainImageMetaInformation: domain.ImageMetaInformation,
                                                 language: Option[String]): api.ImageMetaInformationV2 = {
      asImageMetaInformationV2(domainImageMetaInformation,
                               language,
                               ImageApiProperties.ImageApiUrlBase,
                               Some(ImageApiProperties.RawImageUrlBase))
    }

    private def asApiEditorNotes(notes: Seq[domain.EditorNote]): Seq[api.EditorNote] = {
      notes.map(n => api.EditorNote(n.timeStamp, n.updatedBy, n.note))
    }

    private[service] def asImageMetaInformationV2(imageMeta: domain.ImageMetaInformation,
                                                  language: Option[String],
                                                  baseUrl: String,
                                                  rawBaseUrl: Option[String]): api.ImageMetaInformationV2 = {
      val title = findByLanguageOrBestEffort(imageMeta.titles, language)
        .map(asApiImageTitle)
        .getOrElse(api.ImageTitle("", DefaultLanguage))
      val alttext = findByLanguageOrBestEffort(imageMeta.alttexts, language)
        .map(asApiImageAltText)
        .getOrElse(api.ImageAltText("", DefaultLanguage))
      val tags = findByLanguageOrBestEffort(imageMeta.tags, language)
        .map(asApiImageTag)
        .getOrElse(api.ImageTag(Seq(), DefaultLanguage))
      val caption = findByLanguageOrBestEffort(imageMeta.captions, language)
        .map(asApiCaption)
        .getOrElse(api.ImageCaption("", DefaultLanguage))

      val apiUrl = asApiUrl(imageMeta.imageUrl, rawBaseUrl)

      val editorNotes = Option.when(authRole.userHasWriteRole())(asApiEditorNotes(imageMeta.editorNotes))

      api.ImageMetaInformationV2(
        imageMeta.id.get.toString,
        baseUrl + imageMeta.id.get,
        title,
        alttext,
        apiUrl,
        imageMeta.size,
        imageMeta.contentType,
        withAgreementCopyright(asApiCopyright(imageMeta.copyright)),
        tags,
        caption,
        getSupportedLanguages(imageMeta),
        imageMeta.created,
        imageMeta.createdBy,
        imageMeta.modelReleased.toString,
        editorNotes
      )
    }

    def withAgreementCopyright(image: domain.ImageMetaInformation): domain.ImageMetaInformation = {
      val agreementCopyright = image.copyright.agreementId
        .flatMap(aid => draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright))
        .getOrElse(image.copyright)

      image.copy(
        copyright = image.copyright.copy(
          license = agreementCopyright.license,
          creators = agreementCopyright.creators,
          rightsholders = agreementCopyright.rightsholders,
          validFrom = agreementCopyright.validFrom,
          validTo = agreementCopyright.validTo
        ))
    }

    def withAgreementCopyright(copyright: api.Copyright): api.Copyright = {
      val agreementCopyright =
        copyright.agreementId.flatMap(aid => draftApiClient.getAgreementCopyright(aid)).getOrElse(copyright)
      copyright.copy(
        license = agreementCopyright.license,
        creators = agreementCopyright.creators,
        rightsholders = agreementCopyright.rightsholders,
        validFrom = agreementCopyright.validFrom,
        validTo = agreementCopyright.validTo
      )
    }

    def asApiImageTag(domainImageTag: domain.ImageTag): api.ImageTag = {
      api.ImageTag(domainImageTag.tags, domainImageTag.language)
    }

    def asApiCaption(domainImageCaption: domain.ImageCaption): api.ImageCaption =
      api.ImageCaption(domainImageCaption.caption, domainImageCaption.language)

    def asApiImageTitle(domainImageTitle: domain.ImageTitle): api.ImageTitle = {
      api.ImageTitle(domainImageTitle.title, domainImageTitle.language)
    }

    def asApiLicense(license: String): api.License = {
      getLicense(license)
        .map(l => api.License(l.license.toString, l.description, l.url))
        .getOrElse(api.License("unknown", "", None))
    }

    def asApiUrl(url: String, baseUrl: Option[String] = None): String = {
      val pathToAdd = UrlPath.parse("/" + url.dropWhile(_ == '/'))
      val base = baseUrl.getOrElse("")
      val basePath = base.path.addParts(pathToAdd.parts)
      base.withPath(basePath).toString
    }

    def withNewImage(imageMeta: domain.ImageMetaInformation, image: domain.Image) = {
      val user = authUser.userOrClientid()
      val now = clock.now()
      val newNote = domain.EditorNote(now, user, "Updated image file.")
      imageMeta.copy(
        imageUrl = Uri.parse(image.fileName).toString,
        size = image.size,
        contentType = image.contentType,
        editorNotes = imageMeta.editorNotes :+ newNote
      )
    }

    def asDomainImageMetaInformationV2(imageMeta: api.NewImageMetaInformationV2,
                                       image: domain.Image): Try[domain.ImageMetaInformation] = {
      val modelReleasedStatus = imageMeta.modelReleased match {
        case Some(mrs) => ModelReleasedStatus.valueOfOrError(mrs)
        case None      => Success(ModelReleasedStatus.NOT_SET)
      }

      modelReleasedStatus.map(modelStatus => {
        val now = clock.now()
        val user = authUser.userOrClientid()

        domain.ImageMetaInformation(
          id = None,
          titles = Seq(asDomainTitle(imageMeta.title, imageMeta.language)),
          alttexts = Seq(asDomainAltText(imageMeta.alttext, imageMeta.language)),
          imageUrl = Uri.parse(image.fileName).toString,
          size = image.size,
          contentType = image.contentType,
          copyright = toDomainCopyright(imageMeta.copyright),
          tags = if (imageMeta.tags.nonEmpty) Seq(toDomainTag(imageMeta.tags, imageMeta.language)) else Seq.empty,
          captions = Seq(domain.ImageCaption(imageMeta.caption, imageMeta.language)),
          updatedBy = user,
          createdBy = user,
          created = now,
          updated = now,
          modelReleased = modelStatus,
          editorNotes = Seq(domain.EditorNote(now, user, "Image created."))
        )
      })
    }

    def asDomainTitle(title: String, language: String): domain.ImageTitle = {
      domain.ImageTitle(title, language)
    }

    def asDomainAltText(alt: String, language: String): domain.ImageAltText = {
      domain.ImageAltText(alt, language)
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(
        copyright.license.license,
        copyright.origin,
        copyright.creators.map(toDomainAuthor),
        copyright.processors.map(toDomainAuthor),
        copyright.rightsholders.map(toDomainAuthor),
        copyright.agreementId,
        copyright.validFrom,
        copyright.validTo
      )
    }

    def toDomainAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def toDomainTag(tags: Seq[String], language: String): domain.ImageTag = {
      domain.ImageTag(tags, language)
    }

    def toDomainCaption(caption: String, language: String): domain.ImageCaption = {
      domain.ImageCaption(caption, language)
    }

    def withoutLanguage(domainMetaInformation: domain.ImageMetaInformation,
                        languageToRemove: String): domain.ImageMetaInformation = {
      val now = clock.now()
      val userId = authUser.userOrClientid()
      domainMetaInformation.copy(
        titles = domainMetaInformation.titles.filterNot(_.language == languageToRemove),
        alttexts = domainMetaInformation.alttexts.filterNot(_.language == languageToRemove),
        tags = domainMetaInformation.tags.filterNot(_.language == languageToRemove),
        captions = domainMetaInformation.captions.filterNot(_.language == languageToRemove),
        editorNotes = domainMetaInformation.editorNotes :+ domain.EditorNote(now,
                                                                             userId,
                                                                             s"Deleted language '$languageToRemove'.")
      )
    }

    def getSupportedLanguages(domainImageMetaInformation: domain.ImageMetaInformation): Seq[String] = {
      findSupportedLanguages(
        domainImageMetaInformation.titles,
        domainImageMetaInformation.alttexts,
        domainImageMetaInformation.tags,
        domainImageMetaInformation.captions
      )
    }

  }

}
