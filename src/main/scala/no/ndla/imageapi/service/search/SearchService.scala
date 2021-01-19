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
import no.ndla.imageapi.ImageApiProperties.{ElasticSearchIndexMaxResultWindow, ElasticSearchScrollKeepAlive}
import no.ndla.imageapi.integration.Elastic4sClient
import no.ndla.imageapi.model.api.{Error, ImageMetaSummary}
import no.ndla.imageapi.model.domain.{SearchResult, SearchSettings, Sort}
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats}
import no.ndla.imageapi.model.{Language, NdlaSearchException, ResultWindowTooLargeException}
import no.ndla.mapping.ISO639
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.Formats
import org.json4s.native.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with ImageIndexService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

    def scroll(scrollId: String, language: String): Try[SearchResult] =
      e4sClient
        .execute {
          searchScroll(scrollId, ElasticSearchScrollKeepAlive)
        }
        .map(response => {
          val hits = getHits(response.result, Some(language))
          SearchResult(
            totalCount = response.result.totalHits,
            page = None,
            pageSize = response.result.hits.hits.length,
            language = if (language == "*") Language.AllLanguages else language,
            results = hits,
            scrollId = response.result.scrollId
          )
        })

    def createEmptyIndexIfNoIndexesExist(): Unit = {
      val noIndexesExist =
        imageIndexService.findAllIndexes(ImageApiProperties.SearchIndex).map(_.isEmpty).getOrElse(true)
      if (noIndexesExist) {
        imageIndexService.createIndexWithGeneratedName match {
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
          val resultArray = response.hits.hits.toList
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
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Asc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").sortOrder(SortOrder.Asc).missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" => fieldSort("defaultTitle").sortOrder(SortOrder.Desc).missing("_last")
            case _ =>
              fieldSort(s"titles.$sortLanguage.raw").nestedPath("titles").sortOrder(SortOrder.Desc).missing("_last")
          }
        case Sort.ByRelevanceAsc    => fieldSort("_score").sortOrder(SortOrder.Asc)
        case Sort.ByRelevanceDesc   => fieldSort("_score").sortOrder(SortOrder.Desc)
        case Sort.ByLastUpdatedAsc  => fieldSort("lastUpdated").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByLastUpdatedDesc => fieldSort("lastUpdated").sortOrder(SortOrder.Desc).missing("_last")
        case Sort.ByIdAsc           => fieldSort("id").sortOrder(SortOrder.Asc).missing("_last")
        case Sort.ByIdDesc          => fieldSort("id").sortOrder(SortOrder.Desc).missing("_last")
      }
    }

    def hitAsImageMetaSummary(hit: String, language: Option[String]): ImageMetaSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
      searchConverterService.asImageMetaSummary(read[SearchableImage](hit), language)
    }

    def matchingQuery(settings: SearchSettings): Try[SearchResult] = {
      val fullSearch = settings.query match {
        case Some(query) =>
          val language = settings.language match {
            case Some(lang) if ISO639.languagePriority.contains(lang) => lang
            case _                                                    => "*"
          }

          boolQuery()
            .must(
              boolQuery()
                .should(
                  simpleStringQuery(query).field(s"titles.$language", 2),
                  simpleStringQuery(query).field(s"alttexts.$language", 1),
                  simpleStringQuery(query).field(s"caption.$language", 2),
                  simpleStringQuery(query).field(s"tags.$language", 2),
                  simpleStringQuery(query).field("contributors", 1),
                  idsQuery(query)
                )
            )
        case None => boolQuery()
      }

      executeSearch(fullSearch, settings)
    }

    def executeSearch(queryBuilder: BoolQuery, settings: SearchSettings): Try[SearchResult] = {

      val licenseFilter = settings.license match {
        case None      => if (!settings.includeCopyrighted) Some(noCopyright) else None
        case Some(lic) => Some(termQuery("license", lic))
      }

      val sizeFilter = settings.minimumSize match {
        case Some(size) => Some(rangeQuery("imageSize").gte(size))
        case _          => None
      }

      val (languageFilter, searchLanguage) = settings.language match {
        case None | Some(Language.AllLanguages) =>
          (None, "*")
        case Some(lang) =>
          (Some(existsQuery(s"titles.$lang")), lang)
      }

      val filters = List(languageFilter, licenseFilter, sizeFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)

      val (startAt, numResults) = getStartAtAndNumResults(settings.page, settings.pageSize)
      val requestedResultWindow = settings.page.getOrElse(1) * numResults
      if (requestedResultWindow > ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are $ElasticSearchIndexMaxResultWindow, user requested $requestedResultWindow")
        Failure(new ResultWindowTooLargeException(Error.WindowTooLargeError.description))
      } else {
        val searchToExecute =
          search(ImageApiProperties.SearchIndex)
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
                getHits(response.result, settings.language),
                response.result.scrollId
              ))
          case Failure(ex) => errorHandler(ex)
        }
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

    private def errorHandler[T](exception: Throwable): Failure[T] = {
      exception match {
        case e: NdlaSearchException =>
          e.rf.status match {
            case notFound: Int if notFound == 404 =>
              logger.error(s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              Failure(
                new IndexNotFoundException(s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex"))
            case _ =>
              logger.error(e.getMessage)
              Failure(
                new ElasticsearchException(s"Unable to execute search in ${ImageApiProperties.SearchIndex}",
                                           e.getMessage))
          }
        case t => Failure(t)
      }
    }

    private def scheduleIndexDocuments(): Unit = {
      val f = Future {
        imageIndexService.indexDocuments
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
