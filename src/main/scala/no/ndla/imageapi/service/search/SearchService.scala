/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClient
import no.ndla.imageapi.model.Language
import no.ndla.imageapi.model.api.{ImageMetaSummary, SearchResult}
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats}
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, MatchQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.json4s.native.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SearchService {
  this: ElasticClient with IndexBuilderService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    val noCopyright = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("license","copyrighted"))

    def getHits(response: JestSearchResult): Seq[ImageMetaSummary] = {
      var resultList = Seq[ImageMetaSummary]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while(iterator.hasNext) {
            resultList = resultList :+ hitAsImageMetaSummary(iterator.next().asInstanceOf[JsonObject].get("_source").toString)
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsImageMetaSummary(hit: String): ImageMetaSummary = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      searchConverterService.asImageMetaSummary(read[SearchableImage](hit))
    }

    def languageSpecificSearch(searchField: String, language: Option[String], query: String): QueryBuilder = {
      language match {
        case Some(lang) => QueryBuilders.nestedQuery(searchField, QueryBuilders.matchQuery(s"$searchField.$lang", query).operator(MatchQueryBuilder.Operator.AND))
        case None => {
          Language.supportedLanguages.foldLeft(QueryBuilders.boolQuery())((result, lang) => {
            result.should(QueryBuilders.nestedQuery(searchField, QueryBuilders.matchQuery(s"$searchField.$lang", query).operator(MatchQueryBuilder.Operator.AND)))
          })
        }
      }
    }

    def matchingQuery(query: String, minimumSize: Option[Int], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val fullSearch = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(languageSpecificSearch("titles", language, query))
          .should(languageSpecificSearch("alttexts", language, query))
          .should(languageSpecificSearch("captions", language, query))
          .should(languageSpecificSearch("tags", language, query)))

      executeSearch(fullSearch, minimumSize, license, page, pageSize)
    }

    def all(minimumSize: Option[Int], license: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      executeSearch(
        QueryBuilders.boolQuery(),
        minimumSize,
        license,
        page,
        pageSize)
    }

    def executeSearch(queryBuilder: BoolQueryBuilder, minimumSize: Option[Int], license: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val licensedFiltered = license match {
        case None => queryBuilder.filter(noCopyright)
        case Some(lic) => queryBuilder.filter(QueryBuilders.termQuery("license", lic))
      }

      val sizeFiltered = minimumSize match {
        case None => licensedFiltered
        case Some(size) => licensedFiltered.filter(QueryBuilders.rangeQuery("imageSize").gte(minimumSize.get))
      }

      val search = new SearchSourceBuilder().query(sizeFiltered).sort(SortBuilders.fieldSort("id"))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(search.toString).addIndex(ImageApiProperties.SearchIndex).setParameter(Parameters.SIZE, numResults).setParameter("from", startAt).build()
      val response = jestClient.execute(request)
      response.isSucceeded match {
        case true => SearchResult(response.getTotal.toLong, page.getOrElse(1), numResults, getHits(response))
        case false => errorHandler(response)
      }
    }

    def countDocuments(): Int = {
      jestClient.execute(
        new Count.Builder().addIndex(ImageApiProperties.SearchIndex).build()
      ).getCount.toInt
    }

    private def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(ImageApiProperties.MaxPageSize) else ImageApiProperties.DefaultPageSize
        case None => ImageApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None => 0
      }

      (startAt, numResults)
    }

    private def errorHandler(response: JestSearchResult) = {
      response.getResponseCode match {
        case notFound: Int if notFound == 404 => {
          logger.error(s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex.")
          scheduleIndexDocuments()
          throw new IndexNotFoundException(s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex")
        }
        case _ => throw new ElasticsearchException(s"Unable to execute search in ${ImageApiProperties.SearchIndex}. ErrorMessage: {}", response.getErrorMessage)
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        indexBuilderService.buildIndex()
      }
      f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
    }
  }
}