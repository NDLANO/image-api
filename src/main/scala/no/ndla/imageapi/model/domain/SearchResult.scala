/*
 * Part of NDLA image-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.domain

case class SearchResult[T](
    totalCount: Long,
    page: Option[Int],
    pageSize: Int,
    language: String,
    results: Seq[T],
    scrollId: Option[String]
)
