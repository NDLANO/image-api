/*
 * Part of NDLA image-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.api.ImageMetaSummary

case class SearchResult(
    totalCount: Long,
    page: Option[Int],
    pageSize: Int,
    language: String,
    results: Seq[ImageMetaSummary],
    scrollId: Option[String]
)
