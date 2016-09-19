/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClientComponent
import no.ndla.imageapi.model.api.{ImageMetaSummary, SearchResult}
import no.ndla.imageapi.repository.SearchIndexerComponent
import no.ndla.network.ApplicationUrl
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{MatchQueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SearchService {
  this: ElasticClientComponent with SearchIndexerComponent =>
  val searchService: ElasticContentSearch

  class ElasticContentSearch extends LazyLogging {
    val noCopyright = QueryBuilders.boolQuery().mustNot(QueryBuilders.nestedQuery("copyright.license", QueryBuilders.termQuery("copyright.license.license","copyrighted")))

    def getHits(response: JestSearchResult): Seq[ImageMetaSummary] = {
      var resultList = Seq[ImageMetaSummary]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while(iterator.hasNext) {
            resultList = resultList :+ hitAsImageMetaSummary(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject])
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsImageMetaSummary(hit: JsonObject): ImageMetaSummary = {
      ImageMetaSummary(
        hit.get("id").getAsString,
        ApplicationUrl.get + hit.getAsJsonObject("images").getAsJsonObject("small").get("url").getAsString,
        ApplicationUrl.get + hit.get("id").getAsString,
        hit.getAsJsonObject("copyright").getAsJsonObject("license").get("license").getAsString)
    }

    def matchingQuery(query: Iterable[String], minimumSize: Option[Int], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val titleSearch = QueryBuilders.matchQuery("titles.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val languageSpecificTitleSearch = language match {
        case None => titleSearch
        case Some(lang) => QueryBuilders.boolQuery().must(titleSearch).must(QueryBuilders.termQuery("titles.language", lang))
      }

      val altTextSearch = QueryBuilders.matchQuery("alttexts.alttext", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val languageSpecificAltTextSearch = language match {
        case None => altTextSearch
        case Some(lang) => QueryBuilders.boolQuery().must(altTextSearch).must(QueryBuilders.termQuery("alttexts.language", lang))
      }

      val tagSearch = QueryBuilders.matchQuery("tags.tags", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val languageSpecificTagSearch = language match {
        case None => tagSearch
        case Some(lang) => QueryBuilders.boolQuery().must(tagSearch).must(QueryBuilders.termQuery("tags.language", lang))
      }

      val filters = QueryBuilders.boolQuery().filter(noCopyright)
      license.foreach(lic => filters.filter(QueryBuilders.nestedQuery("copyright.license", QueryBuilders.termQuery("copyright.license.license", license.get))))
      minimumSize.foreach(ms => filters.filter(QueryBuilders.nestedQuery("images.full", QueryBuilders.rangeQuery("images.full.size").gte(minimumSize.get))))

      val fullSearch = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(QueryBuilders.nestedQuery("titles", languageSpecificTitleSearch))
          .should(QueryBuilders.nestedQuery("alttexts", languageSpecificAltTextSearch))
          .should(QueryBuilders.nestedQuery("tags", languageSpecificTagSearch)))
        .must(filters)

      val searchQuery = new SearchSourceBuilder().query(fullSearch).sort(SortBuilders.fieldSort("id"))
      executeSearch(searchQuery, page, pageSize)
    }

    def all(minimumSize: Option[Int], license: Option[String], page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val filters = QueryBuilders.boolQuery().filter(noCopyright)
      license.foreach(lic => filters.filter(QueryBuilders.nestedQuery("copyright.license", QueryBuilders.termQuery("copyright.license.license", license.get))))
      minimumSize.foreach(ms => filters.filter(QueryBuilders.nestedQuery("images.full", QueryBuilders.rangeQuery("images.full.size").gte(minimumSize.get))))

      val searchQuery = new SearchSourceBuilder().query(filters).sort(SortBuilders.fieldSort("id"))
      executeSearch(searchQuery, page, pageSize)
    }

    def executeSearch(searchQuery: SearchSourceBuilder, page: Option[Int], pageSize: Option[Int]): SearchResult = {
      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString).addIndex(ImageApiProperties.SearchIndex).setParameter(Parameters.SIZE, numResults).setParameter("from", startAt).build()
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
        case _ => throw new ElasticsearchException(s"Unable to execute search in ${ImageApiProperties.SearchIndex}", response.getErrorMessage)
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexer.indexDocuments()
      }
      f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
    }
  }
}