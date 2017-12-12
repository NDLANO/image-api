/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.search

import java.util.Date

case class LanguageValue[T](lang: String, value: T)

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])

case class SearchableImage (id: Long,
                            titles: SearchableLanguageValues,
                            alttexts: SearchableLanguageValues,
                            captions: SearchableLanguageValues,
                            tags: SearchableLanguageList,
                            contributors: Seq[String],
                            license: String,
                            imageSize: Long,
                            previewUrl: String,
                            lastUpdated: Date
                           )
