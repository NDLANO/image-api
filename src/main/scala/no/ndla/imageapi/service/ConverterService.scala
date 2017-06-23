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
import no.ndla.imageapi.model.{api, domain}
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

  }
}
