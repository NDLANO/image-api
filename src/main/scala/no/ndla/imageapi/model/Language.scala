/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model

import com.sksamuel.elastic4s.analyzers._
import no.ndla.imageapi.ImageApiProperties.DefaultLanguage
import no.ndla.imageapi.model.domain.LanguageField
import no.ndla.language.model.LanguageTag
import no.ndla.mapping.ISO639

object Language {
  val UnknownLanguage: LanguageTag = LanguageTag("und") // Undefined
  val DefaultLang: LanguageTag = LanguageTag(DefaultLanguage)
  val AllLanguages = "all"
  val NoLanguage = ""

  val languageAnalyzers = Seq(
    LanguageAnalyzer(LanguageTag("nb"), NorwegianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("nn"), NorwegianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("sma"), StandardAnalyzer), // Southern sami
    LanguageAnalyzer(LanguageTag("se"), StandardAnalyzer), // Northern Sami
    LanguageAnalyzer(LanguageTag("en"), EnglishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ar"), ArabicLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("hy"), ArmenianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("eu"), BasqueLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("pt-br"), BrazilianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("bg"), BulgarianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ca"), CatalanLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ja"), CjkLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ko"), CjkLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("zh"), CjkLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("cs"), CzechLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("da"), DanishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("nl"), DutchLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("fi"), FinnishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("fr"), FrenchLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("gl"), GalicianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("de"), GermanLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("el"), GreekLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("hi"), HindiLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("hu"), HungarianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("id"), IndonesianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ga"), IrishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("it"), ItalianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("lt"), LithuanianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("lv"), LatvianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("fa"), PersianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("pt"), PortugueseLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ro"), RomanianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("ru"), RussianLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("srb"), SoraniLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("es"), SpanishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("sv"), SwedishLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("th"), ThaiLanguageAnalyzer),
    LanguageAnalyzer(LanguageTag("tr"), TurkishLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, StandardAnalyzer)
  )

  val supportedLanguages: Seq[LanguageTag] = languageAnalyzers.map(_.languageTag)

  def findByLanguageOrBestEffort[P <: LanguageField[_]](sequence: Seq[P], language: Option[String]): Option[P] =
    language
      .flatMap(lang => sequence.find(_.language == lang))
      .orElse(
        sequence
          .sortBy(lf => {
            supportedLanguages.map(_.toString()).reverse.indexOf(lf.language)
          })
          .lastOption)

  def languageOrUnknown(language: Option[String]): LanguageTag = {
    language.filter(_.nonEmpty) match {
      case Some(x) if x == "unknown" => UnknownLanguage
      case Some(x)                   => LanguageTag(x)
      case None                      => UnknownLanguage
    }
  }

  def findSupportedLanguages[_](fields: Seq[LanguageField[_]]*): Seq[String] = {
    val supportedLanguages = fields.flatMap(languageFields => languageFields.map(lf => lf.language)).distinct
    supportedLanguages.sortBy { lang =>
      ISO639.languagePriority.indexOf(lang)
    }
  }
}

case class LanguageAnalyzer(languageTag: LanguageTag, analyzer: Analyzer)
