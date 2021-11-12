/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.search

import java.util.Date

import no.ndla.imageapi.model.domain.LanguageField

case class LanguageValue[T](language: String, value: T) extends LanguageField[T]

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])

case class SearchableImage(
    id: Long,
    titles: SearchableLanguageValues,
    alttexts: SearchableLanguageValues,
    captions: SearchableLanguageValues,
    tags: SearchableLanguageList,
    contributors: Seq[String],
    license: String,
    imageSize: Long,
    previewUrl: String,
    lastUpdated: Date,
    defaultTitle: Option[String],
    modelReleased: Option[String],
    editorNotes: Seq[String],
    fileType: String,
    width: Number,
    height: Number
)
