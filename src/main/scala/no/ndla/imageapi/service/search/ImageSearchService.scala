/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.ImageApiProperties.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}
import no.ndla.imageapi.auth.Role
import no.ndla.imageapi.integration.Elastic4sClient
import no.ndla.imageapi.model.{Language, ResultWindowTooLargeException}
import no.ndla.imageapi.model.api.{Error, ImageMetaSummary}
import no.ndla.imageapi.model.domain.{SearchResult, SearchSettings, Sort}
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats}
import no.ndla.mapping.ISO639
import org.json4s.native.Serialization.read
import org.json4s.Formats

import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

trait ImageSearchService {
  this: Elastic4sClient with ImageIndexService with SearchService with SearchConverterService with Role =>
  val imageSearchService: ImageSearchService

  class ImageSearchService extends LazyLogging with SearchService[ImageMetaSummary] {
    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))
    override val searchIndex = ImageApiProperties.SearchIndex
    override val indexService = imageIndexService

    def hitToApiModel(hit: String, language: String): ImageMetaSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      searchConverterService.asImageMetaSummary(read[SearchableImage](hit), language)
    }

    override def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage | Language.AllLanguages => "*"
        case _                                           => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          sortLanguage match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Asc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw").sortOrder(SortOrder.Asc).missing("_last")
          }
        case Sort.ByTitleDesc =>
          sortLanguage match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Desc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw").sortOrder(SortOrder.Desc).missing("_last")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").sortOrder(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").sortOrder(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").sortOrder(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").sortOrder(SortOrder.Desc).missing("_last")
      }
    }

    def matchingQuery(settings: SearchSettings): Try[SearchResult[ImageMetaSummary]] = {
      val fullSearch = settings.query match {
        case None => boolQuery()
        case Some(query) =>
          val language = settings.language match {
            case Some(lang) if ISO639.languagePriority.contains(lang) => lang
            case _                                                    => "*"
          }

          val queries = Seq(
            simpleStringQuery(query).field(s"titles.$language", 2),
            simpleStringQuery(query).field(s"alttexts.$language", 1),
            simpleStringQuery(query).field(s"caption.$language", 2),
            simpleStringQuery(query).field(s"tags.$language", 2),
            simpleStringQuery(query).field("contributors", 1),
            idsQuery(query)
          )

          val maybeNoteQuery = Option.when(authRole.userHasWriteRole()) {
            simpleStringQuery(query).field("editorNotes", 1)
          }

          val flattenedQueries = Seq(maybeNoteQuery, queries).flatten
          boolQuery().must(boolQuery().should(flattenedQueries))
      }
      executeSearch(fullSearch, settings)
    }

    def executeSearch(queryBuilder: BoolQuery, settings: SearchSettings): Try[SearchResult[ImageMetaSummary]] = {

      val licenseFilter = settings.license match {
        case None      => Option.unless(settings.includeCopyrighted)(noCopyright)
        case Some(lic) => Some(termQuery("license", lic))
      }

      val sizeFilter = settings.minimumSize match {
        case Some(size) => Some(rangeQuery("imageSize").gte(size))
        case _          => None
      }

      val (languageFilter, searchLanguage) = settings.language match {
        case Some(lang) if Language.supportedLanguages.contains(lang) => (Some(existsQuery(s"titles.$lang")), lang)
        case _                                                        => (None, "*")
      }

      val modelReleasedFilter = Option.when(settings.modelReleased.nonEmpty)(
        boolQuery().should(settings.modelReleased.map(mrs => termQuery("modelReleased", mrs.toString)))
      )

      val filters = List(languageFilter, licenseFilter, sizeFilter, modelReleasedFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException(Error.WindowTooLargeError.description))
      } else {
        val searchToExecute =
          search(searchIndex)
            .size(numResults)
            .from(startAt)
            .highlighting(highlight("*"))
            .query(filteredSearch)
            .sortBy(getSortDefinition(settings.sort, searchLanguage))

        // Only add scroll param if it is first page
        val searchWithScroll =
          if (startAt == 0 && settings.shouldScroll) {
            searchToExecute.scroll(ElasticSearchScrollKeepAlive)
          } else { searchToExecute }

        e4sClient
          .execute(searchWithScroll) match {
          case Success(response) =>
            Success(
              SearchResult(
                response.result.totalHits,
                Some(settings.page.getOrElse(1)),
                numResults,
                if (searchLanguage == "*") Language.AllLanguages else searchLanguage,
                getHits(response.result, searchLanguage),
                response.result.scrollId
              ))
          case Failure(ex) => errorHandler(ex)
        }
      }
    }
  }
}
