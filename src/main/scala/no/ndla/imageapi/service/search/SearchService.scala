/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.{BoolQuery, Query}
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.Elastic4sClient
import no.ndla.imageapi.model.api.{Error, ImageMetaSummary, SearchResult}
import no.ndla.imageapi.model.domain.Sort
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats}
import no.ndla.imageapi.model.{Language, NdlaSearchException, ResultWindowTooLargeException}
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchService {
  this: Elastic4sClient with IndexBuilderService with IndexService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

    def createEmptyIndexIfNoIndexesExist(): Unit = {
      val noIndexesExist = indexService.findAllIndexes(ImageApiProperties.SearchIndex).map(_.isEmpty).getOrElse(true)
      if (noIndexesExist) {
        indexBuilderService.createEmptyIndex match {
          case Success(_) =>
            logger.info("Created empty index")
            scheduleIndexDocuments()
          case Failure(f) =>
            logger.error(s"Failed to create empty index: $f")
        }
      } else {
        logger.info("Existing index(es) kept intact")
      }
    }

    def getHits(response: SearchResponse, language: Option[String]): Seq[ImageMetaSummary] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits
          resultArray.map(result => {
            val matchedLanguage = language match {
              case Some(Language.AllLanguages) | Some("*") | None =>
                searchConverterService.getLanguageFromHit(result).orElse(language)
              case _ => language
            }

            hitAsImageMetaSummary(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage | Language.AllLanguages => "*"
        case _                                           => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.ASC).missing("_last")
            case _   => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.ASC).missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.DESC).missing("_last")
            case _   => fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").order(SortOrder.DESC).missing("_last")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").order(SortOrder.ASC)
        case Sort.ByRelevanceDesc   => fieldSort("_score").order(SortOrder.DESC)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def hitAsImageMetaSummary(hit: String, language: Option[String]): ImageMetaSummary = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      searchConverterService.asImageMetaSummary(read[SearchableImage](hit), language)
    }

    private def languageSpecificSearch(searchField: String,
                                       language: Option[String],
                                       query: String,
                                       boost: Float): Query = {
      language match {
        case None | Some(Language.AllLanguages) =>
          val searchQuery = simpleStringQuery(query).field(s"$searchField.*", 1)
          nestedQuery(searchField, searchQuery).scoreMode(ScoreMode.Avg).boost(boost)
        case Some(lang) =>
          val searchQuery = simpleStringQuery(query).field(s"$searchField.$lang", 1)
          nestedQuery(searchField, searchQuery).scoreMode(ScoreMode.Avg).boost(boost)
      }
    }

    def matchingQuery(query: String,
                      minimumSize: Option[Int],
                      language: Option[String],
                      license: Option[String],
                      sort: Sort.Value,
                      page: Option[Int],
                      pageSize: Option[Int],
                      includeCopyrighted: Boolean): SearchResult = {
      val fullSearch = boolQuery()
        .must(
          boolQuery()
            .should(
              languageSpecificSearch("titles", language, query, 2),
              languageSpecificSearch("alttexts", language, query, 1),
              languageSpecificSearch("captions", language, query, 2),
              languageSpecificSearch("tags", language, query, 2),
              simpleStringQuery(query).field("contributors", 1)
            )
        )

      executeSearch(fullSearch, minimumSize, license, language, sort: Sort.Value, page, pageSize, includeCopyrighted)
    }

    def all(minimumSize: Option[Int],
            license: Option[String],
            language: Option[String],
            sort: Sort.Value,
            page: Option[Int],
            pageSize: Option[Int],
            includeCopyrighted: Boolean): SearchResult =
      executeSearch(boolQuery(), minimumSize, license, language, sort, page, pageSize, includeCopyrighted)

    def executeSearch(queryBuilder: BoolQuery,
                      minimumSize: Option[Int],
                      license: Option[String],
                      language: Option[String],
                      sort: Sort.Value,
                      page: Option[Int],
                      pageSize: Option[Int],
                      includeCopyrighted: Boolean): SearchResult = {

      val licenseFilter = license match {
        case None      => if (!includeCopyrighted) Some(noCopyright) else None
        case Some(lic) => Some(termQuery("license", lic))
      }

      val sizeFilter = minimumSize match {
        case Some(size) => Some(rangeQuery("imageSize").gte(size))
        case _          => None
      }

      val (languageFilter, searchLanguage) = language match {
        case None | Some(Language.AllLanguages) =>
          (None, "*")
        case Some(lang) =>
          (Some(nestedQuery("titles", existsQuery(s"titles.$lang")).scoreMode(ScoreMode.Avg)), lang)
      }

      val filters = List(licenseFilter, sizeFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = page.getOrElse(1) * numResults
      if (requestedResultWindow > ImageApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${ImageApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }

      e4sClient.execute {
        search(ImageApiProperties.SearchIndex)
          .size(numResults)
          .from(startAt)
          .highlighting(highlight("*"))
          .query(filteredSearch)
          .sortBy(getSortDefinition(sort, searchLanguage))
      } match {
        case Success(response) =>
          SearchResult(
            response.result.totalHits,
            page.getOrElse(1),
            numResults,
            if (searchLanguage == "*") Language.AllLanguages else searchLanguage,
            getHits(response.result, language)
          )
        case Failure(ex) =>
          errorHandler(Failure(ex))
      }

    }

    def countDocuments(): Long = {
      val response = e4sClient.execute {
        catCount(ImageApiProperties.SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    private def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(ImageApiProperties.MaxPageSize) else ImageApiProperties.DefaultPageSize
        case None => ImageApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None     => 0
      }

      (startAt, numResults)
    }

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) => {
          e.rf.status match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(
                s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${ImageApiProperties.SearchIndex}",
                                               e.getMessage)
            }
          }

        }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        indexBuilderService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }
}
