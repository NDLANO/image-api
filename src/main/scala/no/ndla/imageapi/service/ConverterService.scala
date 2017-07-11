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
import no.ndla.imageapi.model.Language.{AllLanguages, DefaultLanguage, NoLanguage}
import no.ndla.imageapi.model.{api, domain}
import no.ndla.imageapi.model.domain.LanguageField
import no.ndla.network.ApplicationUrl


trait ConverterService {
  this: User with Clock =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def asApiAuthor(domainAuthor: domain.Author): api.Author = {
      api.Author(domainAuthor.`type`, domainAuthor.name)
    }

    def asApiCopyright(domainCopyright: domain.Copyright): api.Copyright = {
      api.Copyright(asApiLicense(domainCopyright.license), domainCopyright.origin, domainCopyright.authors.map(asApiAuthor))
    }

    def asApiImage(domainImage: domain.Image, baseUrl: Option[String] = None): api.Image = {
      api.Image(baseUrl.getOrElse("") + domainImage.fileName, domainImage.size, domainImage.contentType)
    }

    def asApiImageAltText(domainImageAltText: domain.ImageAltText): api.ImageAltText = {
      api.ImageAltText(domainImageAltText.alttext, domainImageAltText.language)
    }

    def asApiImageMetaInformationWithApplicationUrl(domainImageMetaInformation: domain.ImageMetaInformation): api.ImageMetaInformation = {
      val rawPath = ApplicationUrl.get.replace("/v1/images/", "/raw/")
      asApiImageMetaInformation(domainImageMetaInformation, Some(ApplicationUrl.get), Some(rawPath))
    }

    def asApiImageMetaInformationWithDomainUrl(domainImageMetaInformation: domain.ImageMetaInformation): api.ImageMetaInformation = {
      asApiImageMetaInformation(domainImageMetaInformation, Some(ImageApiProperties.ImageApiUrlBase), Some(ImageApiProperties.RawImageUrlBase))
    }

    private def asApiImageMetaInformation(domainImageMetaInformation: domain.ImageMetaInformation, apiBaseUrl: Option[String] = None, rawImageBaseUrl: Option[String] = None): api.ImageMetaInformation = {
      api.ImageMetaInformation(
        domainImageMetaInformation.id.get.toString,
        apiBaseUrl.getOrElse("") + domainImageMetaInformation.id.get,
        domainImageMetaInformation.titles.map(asApiImageTitle),
        domainImageMetaInformation.alttexts.map(asApiImageAltText),
        asApiUrl(domainImageMetaInformation.imageUrl, rawImageBaseUrl),
        domainImageMetaInformation.size,
        domainImageMetaInformation.contentType,
        asApiCopyright(domainImageMetaInformation.copyright),
        domainImageMetaInformation.tags.map(asApiImageTag),
        domainImageMetaInformation.captions.map(asApiCaption))
    }

    def asApiImageMetaInformationWithSingleLanguage(domainImageMetaInformation: domain.ImageMetaInformation, language: String): Option[api.ImageMetaInformationSingleLanguage] = {

      val supportedLanguages = getSupportedLanguages(domainImageMetaInformation)
      val searchLanguage = if (language == AllLanguages) DefaultLanguage else language
      if (!supportedLanguages.contains(searchLanguage)) {
        return None
      }

      def setByLanguage[T](seq: Seq[LanguageField[T]]) = {
        if (language == AllLanguages)
          getByLanguageOrHead[T](seq, searchLanguage)
        else
          findValueByLanguage[T](seq, searchLanguage)
      }

      val rawPath = Some(ApplicationUrl.get.replace("/v2/images/", "/raw/"))
      val title =   setByLanguage[String](domainImageMetaInformation.titles)
      val altText = setByLanguage[String](domainImageMetaInformation.alttexts)
      val caption = setByLanguage[String](domainImageMetaInformation.captions)
      val tags =    setByLanguage[Seq[String]](domainImageMetaInformation.tags)

      if (title.isDefined || altText.isDefined || caption.isDefined || tags.isDefined) {
        val titleString = title.getOrElse("")
        val altTextString = altText.getOrElse("")
        val captionString = caption.getOrElse("")
        val tagsSeq = tags.getOrElse(Seq.empty[String])

        Some(api.ImageMetaInformationSingleLanguage(
          domainImageMetaInformation.id.get.toString,
          Some(ApplicationUrl.get).getOrElse("") + domainImageMetaInformation.id.get,
          language,
          titleString,
          altTextString,
          asApiUrl(domainImageMetaInformation.imageUrl, rawPath),
          domainImageMetaInformation.size,
          domainImageMetaInformation.contentType,
          asApiCopyright(domainImageMetaInformation.copyright),
          tagsSeq,
          captionString,
          supportedLanguages))
      } else {
        None
      }
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
      baseUrl.getOrElse("") + url
    }

    def asDomainImageMetaInformation(imageMeta: api.NewImageMetaInformation, image: domain.Image): domain.ImageMetaInformation = {
      domain.ImageMetaInformation(
        None,
        imageMeta.titles.map(asDomainTitle),
        imageMeta.alttexts.map(asDomainAltText),
        image.fileName,
        image.size,
        image.contentType,
        toDomainCopyright(imageMeta.copyright),
        imageMeta.tags.getOrElse(Seq.empty).map(toDomainTag),
        imageMeta.captions.getOrElse(Seq.empty).map(toDomainCaption),
        authUser.id(),
        clock.now()
      )
    }

    def asDomainTitle(title: api.ImageTitle): domain.ImageTitle = {
      domain.ImageTitle(title.title, title.language)
    }

    def asDomainAltText(alt: api.ImageAltText): domain.ImageAltText = {
      domain.ImageAltText(alt.alttext, alt.language)
    }

    def toDomainCopyright(copyright: api.Copyright): domain.Copyright = {
      domain.Copyright(toDomainLicense(copyright.license), copyright.origin, copyright.authors.map(toDomainAuthor))
    }

    def toDomainLicense(license: api.License): domain.License = {
      domain.License(license.license, license.description, license.url)
    }

    def toDomainAuthor(author: api.Author): domain.Author = {
      domain.Author(author.`type`, author.name)
    }

    def toDomainTag(tag: api.ImageTag): domain.ImageTag = {
      domain.ImageTag(tag.tags, tag.language)
    }

    def toDomainCaption(caption: api.ImageCaption): domain.ImageCaption = {
      domain.ImageCaption(caption.caption, caption.language)
    }

    def getByLanguageOrHead[T](sequence: Seq[LanguageField[T]], language: String): Option[T] = {
      findValueByLanguage(sequence, language) match {
        case Some(e) => Some(e)
        case None => sequence.headOption.map(lf => lf.value)
      }
    }

    def findValueByLanguage[T](sequence: Seq[LanguageField[T]], language: String): Option[T] = {
      sequence.find(_.language.getOrElse("") == language).map(_.value) match {
        case Some(e) => Some(e)
        case None => sequence.find(_.language.getOrElse(DefaultLanguage) == language).map(_.value)
      }
    }

    def getSupportedLanguages(domainImageMetaInformation: domain.ImageMetaInformation): Seq[String] = {
      domainImageMetaInformation.titles.map(_.language.getOrElse(NoLanguage))
        .++:(domainImageMetaInformation.alttexts.map(_.language.getOrElse(NoLanguage)))
        .++:(domainImageMetaInformation.tags.map(_.language.getOrElse(NoLanguage)))
        .++:(domainImageMetaInformation.captions.map(_.language.getOrElse(NoLanguage)))
        .distinct
        .filterNot(lang => lang.isEmpty)
    }

  }
}
