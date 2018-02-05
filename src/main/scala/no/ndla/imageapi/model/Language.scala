/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model

import com.sksamuel.elastic4s.analyzers._
import no.ndla.imageapi.model.domain.LanguageField
import no.ndla.mapping.ISO639

object Language {
  val DefaultLanguage = "nb"
  val UnknownLanguage = "unknown"
  val AllLanguages = "all"
  val NoLanguage = ""

  val languageAnalyzers = Seq(
    LanguageAnalyzer(DefaultLanguage, NorwegianLanguageAnalyzer),
    LanguageAnalyzer("nn", NorwegianLanguageAnalyzer),
    LanguageAnalyzer("en", EnglishLanguageAnalyzer),
    LanguageAnalyzer("fr", FrenchLanguageAnalyzer),
    LanguageAnalyzer("de", GermanLanguageAnalyzer),
    LanguageAnalyzer("es", SpanishLanguageAnalyzer),
    LanguageAnalyzer("se", StandardAnalyzer), // SAMI
    LanguageAnalyzer("zh", ChineseLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, NorwegianLanguageAnalyzer)
  )

  val supportedLanguages = languageAnalyzers.map(_.lang)

  def findByLanguageOrBestEffort[P <: LanguageField[_]](sequence: Seq[P], language: Option[String]): Option[P] = {
    language.flatMap(lang => sequence.find(_.language == lang)).orElse(
      sequence.sortBy(lf => {
        ISO639.languagePriority.reverse.indexOf(lf.language)
      }).lastOption)
  }

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None => UnknownLanguage
    }
  }

  def findSupportedLanguages[_](fields: Seq[LanguageField[_]]*): Seq[String] = {
    val supportedLanguages = fields.flatMap(languageFields => languageFields.map(lf => lf.language)).distinct
    supportedLanguages.sortBy{lang =>
      ISO639.languagePriority.indexOf(lang)
    }
  }
}

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)

