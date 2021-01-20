/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.BoolQuery
import com.sksamuel.elastic4s.searches.sort.SortOrder
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.ImageApiProperties.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}
import no.ndla.imageapi.integration.Elastic4sClient
import no.ndla.imageapi.model.api.Error
import no.ndla.imageapi.model.domain.SearchResult
import no.ndla.imageapi.model.search.SearchableTag
import no.ndla.imageapi.model.{Language, ResultWindowTooLargeException}
import org.json4s._
import org.json4s.native.Serialization.read

import scala.util.{Failure, Success, Try}

trait TagSearchService {
  this: Elastic4sClient
    with SearchConverterService
    with SearchService
    with TagIndexService
    with SearchConverterService =>
  val tagSearchService: TagSearchService

  class TagSearchService extends LazyLogging with SearchService[String] {
    implicit val formats: Formats = DefaultFormats
    override val searchIndex: String = ImageApiProperties.TagSearchIndex
    override val indexService = tagIndexService

    override def hitToApiModel(hit: String, language: Option[String]): String = {
      val searchableTag = read[SearchableTag](hit)
      searchableTag.tag
    }

    def all(
        language: String,
        page: Int,
        pageSize: Int
    ): Try[SearchResult[String]] = executeSearch(language, page, pageSize, boolQuery())

    def matchingQuery(query: String, searchLanguage: String, page: Int, pageSize: Int): Try[SearchResult[String]] = {

      val language = if (searchLanguage == Language.AllLanguages) "*" else searchLanguage

      val fullQuery = boolQuery()
        .must(
          boolQuery().should(
            matchQuery("tag", query).boost(2),
            prefixQuery("tag", query)
          )
        )

      executeSearch(language, page, pageSize, fullQuery)
    }

    def executeSearch(
        language: String,
        page: Int,
        pageSize: Int,
        queryBuilder: BoolQuery,
    ): Try[SearchResult[String]] = {

      val languageFilter =
        if (language == "all" || language == "*") None
        else
          Some(
            termQuery("language", language)
          )

      val filters = List(languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(Some(page), Some(pageSize))
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException(Error.WindowTooLargeError.description))
      } else {
        val searchToExecute = search(searchIndex)
          .size(numResults)
          .from(startAt)
          .query(filteredSearch)
          .sortBy(fieldSort("_score").sortOrder(SortOrder.Desc))

        val searchWithScroll =
          if (startAt != 0) { searchToExecute } else { searchToExecute.scroll(ElasticSearchScrollKeepAlive) }

        e4sClient.execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(page),
                numResults,
                if (language == "*") Language.AllLanguages else language,
                getHits(response.result, Some(language)),
                response.result.scrollId
              ))
          case Failure(ex) =>
            errorHandler(ex)
        }
      }
    }
  }
}
