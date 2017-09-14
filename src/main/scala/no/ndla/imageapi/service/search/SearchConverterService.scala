/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.api.ImageMetaSummary
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.{LanguageValue, SearchableImage, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.network.ApplicationUrl
import no.ndla.imageapi.ImageApiProperties.DefaultLanguage

trait SearchConverterService {
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {
    def asSearchableImage(image: ImageMetaInformation): SearchableImage = {
      SearchableImage(
        id = image.id.get,
        titles = SearchableLanguageValues(image.titles.map(title => LanguageValue(title.language, title.title))),
        alttexts = SearchableLanguageValues(image.alttexts.map(alttext => LanguageValue(alttext.language, alttext.alttext))),
        captions = SearchableLanguageValues(image.captions.map(caption => LanguageValue(caption.language, caption.caption))),
        tags = SearchableLanguageList(image.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        license = image.copyright.license.license,
        imageSize = image.size,
        previewUrl = image.imageUrl)
    }

    def asImageMetaSummary(searchableImage: SearchableImage, language: Option[String]): ImageMetaSummary = {
      val apiToRawRegex = "/v\\d+/images/".r
      val title = searchableImage.titles.languageValues.find(title => title.lang == language.getOrElse(DefaultLanguage))
        .orElse(searchableImage.titles.languageValues.headOption)
        .map(_.value)
      val altText = searchableImage.alttexts.languageValues.find(title => title.lang == language.getOrElse(DefaultLanguage))
        .orElse(searchableImage.alttexts.languageValues.headOption)
        .map(_.value)

      ImageMetaSummary(
        id = searchableImage.id.toString,
        title = title.getOrElse(""),
        altText = altText.getOrElse(""),
        previewUrl = apiToRawRegex.replaceFirstIn(ApplicationUrl.get, "/raw/") + searchableImage.previewUrl,
        metaUrl = ApplicationUrl.get + searchableImage.id,
        license = searchableImage.license)
    }
  }
}
