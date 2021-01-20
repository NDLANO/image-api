/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.ImageApiProperties.ElasticSearchScrollKeepAlive
import no.ndla.imageapi.integration.Elastic4sClient
import no.ndla.imageapi.model.domain.{SearchResult, Sort}
import no.ndla.imageapi.model.{Language, NdlaSearchException}
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SearchService {
  this: Elastic4sClient with IndexService with SearchConverterService =>

  trait SearchService[T] extends LazyLogging {
    val searchIndex: String
    val indexService: IndexService[_, _]

    def hitToApiModel(hit: String, language: Option[String]): T

    def scroll(scrollId: String, language: String): Try[SearchResult[T]] =
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
        indexService.findAllIndexes(searchIndex).map(_.isEmpty).getOrElse(true)
      if (noIndexesExist) {
        indexService.createIndexWithGeneratedName match {
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

    def getHits(response: SearchResponse, language: Option[String]): Seq[T] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits.toList
          resultArray.map(result => {
            val matchedLanguage = language match {
              case Some(Language.AllLanguages) | Some("*") | None =>
                searchConverterService.getLanguageFromHit(result).orElse(language)
              case _ => language
            }

            hitToApiModel(result.sourceAsString, matchedLanguage)
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

    def countDocuments(): Long = {
      val response = e4sClient.execute {
        catCount(searchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

    protected def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
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

    protected def errorHandler[T](exception: Throwable): Failure[T] = {
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

    def scheduleIndexDocuments(): Unit = {
      val f = Future(indexService.indexDocuments)

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents ($searchIndex) in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }
}
