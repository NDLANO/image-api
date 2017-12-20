/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClient
import no.ndla.imageapi.model.api.{Error, ImageMetaSummary, SearchResult}
import no.ndla.imageapi.model.domain.Sort
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats}
import no.ndla.imageapi.model.{Language, NdlaSearchException, ResultWindowTooLargeException}
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query._
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.sort.{SortBuilders, SortOrder}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.collection.JavaConverters._

trait SearchService {
  this: ElasticClient with IndexBuilderService with IndexService with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    private val noCopyright = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("license","copyrighted"))

    def createEmptyIndexIfNoIndexesExist(): Unit = {
      val noIndexesExist = indexService.findAllIndexes.map(_.isEmpty).getOrElse(true)
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

    def getHits(response: JestSearchResult, language: Option[String]): Seq[ImageMetaSummary] = {
      response.getTotal match {
        case count: java.lang.Long if count > 0 =>
          val resultArray = (parse(response.getJsonString) \ "hits" \ "hits").asInstanceOf[JArray].arr

          resultArray.map(result => {
            val matchedLanguage = language match {
              case Some(Language.AllLanguages) | Some("*") | None =>
                searchConverterService.getLanguageFromHit(result).orElse(language)
              case _ => language
            }

            val hitString = compact(render(result \ "_source"))
            hitAsImageMetaSummary(hitString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String) = {
      val sortLanguage = language match {
        case Language.NoLanguage => Language.DefaultLanguage
        case _ => language
      }

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" => SortBuilders.fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _ => SortBuilders.fieldSort(s"titles.$sortLanguage.raw").setNestedPath("titles").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" => SortBuilders.fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _ => SortBuilders.fieldSort(s"titles.$sortLanguage.raw").setNestedPath("titles").order(SortOrder.DESC).missing("_last")
          }
        case (Sort.ByRelevanceAsc) => SortBuilders.fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => SortBuilders.fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByIdAsc) => SortBuilders.fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => SortBuilders.fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def hitAsImageMetaSummary(hit: String, language: Option[String]): ImageMetaSummary = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      searchConverterService.asImageMetaSummary(read[SearchableImage](hit), language)
    }

    private def languageSpecificSearch(searchField: String, language: Option[String], query: String, boost: Float): QueryBuilder = {
      val highlightBuilder = new HighlightBuilder().preTags("").postTags("").field("*").numOfFragments(0)
      val innerHitBuilder = new InnerHitBuilder().setHighlightBuilder(highlightBuilder)

      language match {
        case None | Some(Language.AllLanguages) =>
          val searchQuery = QueryBuilders.simpleQueryStringQuery(query).field(s"$searchField.*")
          QueryBuilders.nestedQuery(searchField, searchQuery, ScoreMode.Avg).boost(boost).innerHit(innerHitBuilder)
        case Some(lang) =>
          val searchQuery = QueryBuilders.simpleQueryStringQuery(query).field(s"$searchField.$lang")
          QueryBuilders.nestedQuery(searchField, searchQuery, ScoreMode.Avg).boost(boost)
      }
    }

    def matchingQuery(query: String, minimumSize: Option[Int], language: Option[String], license: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int], includeCopyrighted: Boolean): SearchResult = {
      val fullSearch = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(languageSpecificSearch("titles", language, query, 2))
          .should(languageSpecificSearch("alttexts", language, query, 1))
          .should(languageSpecificSearch("captions", language, query, 2))
          .should(languageSpecificSearch("tags", language, query, 2))
          .should(QueryBuilders.simpleQueryStringQuery(query).field("contributors")).boost(1))

      executeSearch(fullSearch, minimumSize, license, language, sort: Sort.Value, page, pageSize, includeCopyrighted)
    }

    def all(minimumSize: Option[Int], license: Option[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int], includeCopyrighted: Boolean): SearchResult =
      executeSearch(QueryBuilders.boolQuery(), minimumSize, license, language, sort, page, pageSize, includeCopyrighted)

    def executeSearch(queryBuilder: BoolQueryBuilder, minimumSize: Option[Int], license: Option[String], language: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int], includeCopyrighted: Boolean): SearchResult = {
      val licensedFiltered = license match {
        case None =>  if (!includeCopyrighted) queryBuilder.filter(noCopyright) else queryBuilder
        case Some(lic) => queryBuilder.filter(QueryBuilders.termQuery("license", lic))
      }

      val sizeFiltered = minimumSize match {
        case None => licensedFiltered
        case Some(size) => licensedFiltered.filter(QueryBuilders.rangeQuery("imageSize").gte(minimumSize.get))
      }

      val (languageFiltered, searchLanguage) = language match {
        case None | Some(Language.AllLanguages) =>
          (sizeFiltered, "*")
        case Some(lang) =>
          (sizeFiltered.filter(QueryBuilders.nestedQuery("titles", QueryBuilders.existsQuery(s"titles.$lang"), ScoreMode.Avg)), lang)
      }

      val search = new SearchSourceBuilder().query(languageFiltered).sort(getSortDefinition(sort, searchLanguage))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(search.toString).addIndex(ImageApiProperties.SearchIndex).setParameter(Parameters.SIZE, numResults).setParameter("from", startAt).build()

      val requestedResultWindow = page.getOrElse(1)*numResults
      if(requestedResultWindow > ImageApiProperties.ElasticSearchIndexMaxResultWindow) {
          logger.info(s"Max supported results are ${ImageApiProperties.ElasticSearchIndexMaxResultWindow}, user requested ${requestedResultWindow}")
          throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }

      jestClient.execute(request) match {
        case Success(response) => SearchResult(response.getTotal.toLong, page.getOrElse(1), numResults, getHits(response, language))
        case Failure(f) => errorHandler(Failure(f))
      }
    }

    def countDocuments(): Int = {
      val ret = jestClient.execute(
        new Count.Builder().addIndex(ImageApiProperties.SearchIndex).build()
      ).map(result => result.getCount.toInt)
      ret.getOrElse(0)
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

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) => {
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index ${ImageApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${ImageApiProperties.SearchIndex}", e.getResponse.getErrorMessage)
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
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }
}
