/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.netaporter.uri.Uri.parse
import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties.DefaultLanguage
import no.ndla.imageapi.model.Language
import no.ndla.imageapi.model.api.{ImageAltText, ImageMetaSummary, ImageTitle}
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.{LanguageValue, SearchableImage, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.imageapi.service.ConverterService
import no.ndla.network.ApplicationUrl

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {
    def asSearchableImage(image: ImageMetaInformation): SearchableImage = {
      val imageWithAgreement = converterService.withAgreementCopyright(image)

      val defaultTitle = imageWithAgreement.titles.sortBy(title => {
        val languagePriority = Language.languageAnalyzers.map(la => la.lang).reverse
        languagePriority.indexOf(title.language)
      }).lastOption

      SearchableImage(
        id = imageWithAgreement.id.get,
        titles = SearchableLanguageValues(imageWithAgreement.titles.map(title => LanguageValue(title.language, title.title))),
        alttexts = SearchableLanguageValues(imageWithAgreement.alttexts.map(alttext => LanguageValue(alttext.language, alttext.alttext))),
        captions = SearchableLanguageValues(imageWithAgreement.captions.map(caption => LanguageValue(caption.language, caption.caption))),
        tags = SearchableLanguageList(imageWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        contributors = image.copyright.creators.map(c => c.name) ++ image.copyright.processors.map(p => p.name) ++ image.copyright.rightsholders.map(r => r.name),
        license = imageWithAgreement.copyright.license.license,
        imageSize = imageWithAgreement.size,
        previewUrl = parse(imageWithAgreement.imageUrl).toString,
        lastUpdated = imageWithAgreement.updated,
        defaultTitle = defaultTitle.map(t => t.title)
      )
    }

    def asImageMetaSummary(searchableImage: SearchableImage, language: Option[String]): ImageMetaSummary = {
      val apiToRawRegex = "/v\\d+/images/".r
      val title = searchableImage.titles.languageValues.find(title => title.lang == language.getOrElse(DefaultLanguage))
        .orElse(searchableImage.titles.languageValues.headOption)
        .map(res => ImageTitle(res.value, res.lang))
        .getOrElse(ImageTitle("", DefaultLanguage))
      val altText = searchableImage.alttexts.languageValues.find(title => title.lang == language.getOrElse(DefaultLanguage))
        .orElse(searchableImage.alttexts.languageValues.headOption)
        .map(res => ImageAltText(res.value, res.lang))
        .getOrElse(ImageAltText("", DefaultLanguage))

      ImageMetaSummary(
        id = searchableImage.id.toString,
        title = title,
        contributors = searchableImage.contributors,
        altText = altText,
        previewUrl = apiToRawRegex.replaceFirstIn(ApplicationUrl.get, "/raw") + searchableImage.previewUrl,
        metaUrl = ApplicationUrl.get + searchableImage.id,
        license = searchableImage.license)
    }

    def getLanguageFromHit(result: SearchHit): Option[String] = {
      val sortedInnerHits = result.innerHits.toList.filter(ih => ih._2.total > 0).sortBy{
        case (_,hit) => hit.max_score
      }
      sortedInnerHits.headOption.flatMap{
        case (hitField, innerHit) =>
          val sortedInnerInnerHits = innerHit.hits.sortBy(hit => hit.score).reverse
            sortedInnerInnerHits.headOption.flatMap(hit => {
            //TODO: Test sorting of both innerHits and sorting of innerHit.hits
            hit.highlight.headOption.map(hl => hl._1.split('.').last)
          })
        } match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          result.sourceAsMap.keySet.headOption.map(fieldName => fieldName.split('.').last) //TODO: Maybe sort this by default language priority
      }

    }

  }
}
