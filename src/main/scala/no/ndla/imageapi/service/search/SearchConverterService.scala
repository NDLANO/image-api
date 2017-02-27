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

    def asImageMetaSummary(searchableImage: SearchableImage): ImageMetaSummary = {
      ImageMetaSummary(
        id = searchableImage.id.toString,
        previewUrl = ApplicationUrl.get.replace("/images/", "/raw/") + searchableImage.previewUrl,
        metaUrl = ApplicationUrl.get + searchableImage.id,
        license = searchableImage.license)
    }
  }
}
