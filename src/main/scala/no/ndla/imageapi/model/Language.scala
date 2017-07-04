/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model

import com.sksamuel.elastic4s.analyzers._

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
}

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)

