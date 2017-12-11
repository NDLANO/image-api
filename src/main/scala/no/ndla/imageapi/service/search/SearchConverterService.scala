/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.api.{ImageAltText, ImageMetaSummary, ImageTitle}
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.{LanguageValue, SearchableImage, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.network.ApplicationUrl
import no.ndla.imageapi.ImageApiProperties.DefaultLanguage
import com.netaporter.uri.Uri.parse
import no.ndla.imageapi.service.ConverterService
import org.json4s._
import org.json4s.native.JsonMethods._

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {
    def asSearchableImage(image: ImageMetaInformation): SearchableImage = {
      val imageWithAgreement = converterService.withAgreementCopyright(image)

      SearchableImage(
        id = imageWithAgreement.id.get,
        titles = SearchableLanguageValues(imageWithAgreement.titles.map(title => LanguageValue(title.language, title.title))),
        alttexts = SearchableLanguageValues(imageWithAgreement.alttexts.map(alttext => LanguageValue(alttext.language, alttext.alttext))),
        captions = SearchableLanguageValues(imageWithAgreement.captions.map(caption => LanguageValue(caption.language, caption.caption))),
        tags = SearchableLanguageList(imageWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        contributors = image.copyright.creators.map(c => c.name) ++ image.copyright.processors.map(p => p.name) ++ image.copyright.rightsholders.map(r => r.name),
        license = imageWithAgreement.copyright.license.license,
        imageSize = imageWithAgreement.size,
        previewUrl = parse(imageWithAgreement.imageUrl).toString)
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

    def getLanguageFromHit(jsonObject: JValue): Option[String] = {
      implicit val formats = DefaultFormats

      // Fetches matched language from highlight in innerHit
      // since elasticsearch doesn't return which nested field that matches
      val innerHits = jsonObject \ "inner_hits"

      val sortedInnerHits = innerHits.children.sortBy(innerHit => {
        (innerHit \ "hits" \ "max_score").toOption.map(s => {
          s.extract[Double]
        }).getOrElse[Double](0)
      }).reverse

      sortedInnerHits.headOption.flatMap(innerHit => {
        (innerHit \\ "highlight").extract[Map[String, Any]].keySet.headOption.map(fieldName =>
          fieldName.split('.').last)
      }) match {
        case Some(lang) => Some(lang)
        case _ =>
          (jsonObject \ "_source" \ "titles").extract[Map[String, Any]].keySet.headOption.map(fieldName =>
            fieldName.split('.').last)
      }
    }

  }
}
