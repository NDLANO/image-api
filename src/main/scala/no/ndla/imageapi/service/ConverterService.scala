/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model.Language._
import no.ndla.imageapi.model.{api, domain}
import no.ndla.network.ApplicationUrl
import com.netaporter.uri.Uri.parse
import no.ndla.imageapi.integration.DraftApiClient
import no.ndla.imageapi.model.api.ImageMetaInformationV2

trait ConverterService {
  this: User with Clock with DraftApiClient =>
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
        domainCopyright.agreement,
        domainCopyright.validFrom,
        domainCopyright.validTo)
    }

    def asApiImage(domainImage: domain.Image, baseUrl: Option[String] = None): api.Image = {
      api.Image(baseUrl.getOrElse("") + domainImage.fileName, domainImage.size, domainImage.contentType)
    }

    def asApiImageAltText(domainImageAltText: domain.ImageAltText): api.ImageAltText = {
      api.ImageAltText(domainImageAltText.alttext, domainImageAltText.language)
    }

    def asApiImageMetaInformationWithApplicationUrlV2(domainImageMetaInformation: domain.ImageMetaInformation, language: Option[String]): Option[api.ImageMetaInformationV2] = {
      val rawPath = ApplicationUrl.get.replace("/v2/images/", "/raw")
      asImageMetaInformationV2(domainImageMetaInformation, language, ApplicationUrl.get, Some(rawPath))
    }

    def asApiImageMetaInformationWithDomainUrlV2(domainImageMetaInformation: domain.ImageMetaInformation, language: Option[String]): Option[api.ImageMetaInformationV2] = {
      asImageMetaInformationV2(domainImageMetaInformation, language, ImageApiProperties.ImageApiUrlBase, Some(ImageApiProperties.RawImageUrlBase))
    }

    private def asImageMetaInformationV2(imageMeta: domain.ImageMetaInformation, language: Option[String], baseUrl: String, rawBaseUrl: Option[String]): Option[api.ImageMetaInformationV2] = {
      val title = findByLanguageOrBestEffort(imageMeta.titles, language).map(asApiImageTitle).getOrElse(api.ImageTitle("", DefaultLanguage))
      val alttext = findByLanguageOrBestEffort(imageMeta.alttexts, language).map(asApiImageAltText).getOrElse(api.ImageAltText("", DefaultLanguage))
      val tags = findByLanguageOrBestEffort(imageMeta.tags, language).map(asApiImageTag).getOrElse(api.ImageTag(Seq(), DefaultLanguage))
      val caption = findByLanguageOrBestEffort(imageMeta.captions, language).map(asApiCaption).getOrElse(api.ImageCaption("", DefaultLanguage))

      Some(api.ImageMetaInformationV2(
        imageMeta.id.get.toString,
        baseUrl + imageMeta.id.get,
        title,
        alttext,
        asApiUrl(imageMeta.imageUrl, rawBaseUrl),
        imageMeta.size,
        imageMeta.contentType,
        asApiCopyright(imageMeta.copyright),
        tags,
        caption,
        getSupportedLanguages(imageMeta)))
    }

    def withAgreementCopyright(image: domain.ImageMetaInformation): domain.ImageMetaInformation = {
      val agreementCopyright = image.copyright.agreement.flatMap(aid =>
        draftApiClient.getAgreementCopyright(aid).map(toDomainCopyright)
      ).getOrElse(image.copyright)

      image.copy(copyright = image.copyright.copy(
        license = agreementCopyright.license,
        creators = if (agreementCopyright.creators.nonEmpty) agreementCopyright.creators else image.copyright.creators,
        rightsholders = if (agreementCopyright.rightsholders.nonEmpty) agreementCopyright.rightsholders else image.copyright.rightsholders
      ))
    }

    def withAgreementCopyright(image: ImageMetaInformationV2): ImageMetaInformationV2 = {
      val agreementCopyright = image.copyright.agreement.flatMap(aid => draftApiClient.getAgreementCopyright(aid)).getOrElse(image.copyright)

      image.copy(copyright = image.copyright.copy(
        license = agreementCopyright.license,
        creators = if (agreementCopyright.creators.nonEmpty) agreementCopyright.creators else image.copyright.creators,
        rightsholders = if (agreementCopyright.rightsholders.nonEmpty) agreementCopyright.rightsholders else image.copyright.rightsholders
      ))
    }

    def asApiImageTag(domainImageTag: domain.ImageTag): api.ImageTag = {
      api.ImageTag(domainImageTag.tags, domainImageTag.language)
    }

    def asApiCaption(domainImageCaption: domain.ImageCaption): api.ImageCaption =
      api.ImageCaption(domainImageCaption.caption, domainImageCaption.language)

    def asApiImageTitle(domainImageTitle: domain.ImageTitle): api.ImageTitle = {
      api.ImageTitle(domainImageTitle.title, domainImageTitle.language)
    }

    def asApiLicense(domainLicense: domain.License): api.License = {
      api.License(domainLicense.license, domainLicense.description, domainLicense.url)
    }

    def asApiUrl(url: String, baseUrl: Option[String] = None): String = {
      baseUrl.getOrElse("") + parse(url).toString
    }

    def asDomainImageMetaInformationV2(imageMeta: api.NewImageMetaInformationV2, image: domain.Image): domain.ImageMetaInformation = {
      domain.ImageMetaInformation(
        None,
        Seq(asDomainTitle(imageMeta.title, imageMeta.language)),
        Seq(asDomainAltText(imageMeta.alttext, imageMeta.language)),
        parse(image.fileName).toString,
        image.size,
        image.contentType,
        toDomainCopyright(imageMeta.copyright),
        if (imageMeta.tags.nonEmpty) Seq(toDomainTag(imageMeta.tags, imageMeta.language)) else Seq.empty,
        Seq(domain.ImageCaption(imageMeta.caption, imageMeta.language)),
        authUser.id(),
        clock.now()
      )
    }

    def asDomainTitle(title: String, language: String): domain.ImageTitle = {
      domain.ImageTitle(title, language)
    }

    def asDomainAltText(alt: String, language: String): domain.ImageAltText = {
      domain.ImageAltText(alt, language)
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(
        toDomainLicense(copyright.license),
        copyright.origin,
        copyright.creators.map(toDomainAuthor),
        copyright.processors.map(toDomainAuthor),
        copyright.rightsholders.map(toDomainAuthor),
        copyright.agreement,
        copyright.validFrom,
        copyright.validTo)
    }

    def toDomainLicense(license: api.License): domain.License = {
      domain.License(license.license, license.description, license.url)
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

    def getSupportedLanguages(domainImageMetaInformation: domain.ImageMetaInformation): Seq[String] = {
      domainImageMetaInformation.titles.map(_.language)
        .++:(domainImageMetaInformation.alttexts.map(_.language))
        .++:(domainImageMetaInformation.tags.map(_.language))
        .++:(domainImageMetaInformation.captions.map(_.language))
        .distinct
        .filterNot(lang => lang.isEmpty)
    }

  }

}
