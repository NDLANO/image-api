/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.integration

import java.util

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.business.{SearchIndexer, SearchMeta}
import no.ndla.imageapi.model.ImageMetaSummary
import no.ndla.imageapi.network.ApplicationUrl
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.transport.RemoteTransportException

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ElasticSearchMeta(clusterName:String, clusterHost:String, clusterPort:String) extends SearchMeta with LazyLogging {

  lazy val client = ElasticClient.transport(
    Settings.settingsBuilder().put("cluster.name", clusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://$clusterHost:$clusterPort"))

  val noCopyrightFilter = not(nestedQuery("copyright.license").query(termQuery("copyright.license.license", "copyrighted")))

  implicit object ImageHitAs extends HitAs[ImageMetaSummary] {
    override def as(hit: RichSearchHit): ImageMetaSummary = {
      val sourceMap = hit.sourceAsMap
      ImageMetaSummary(
        sourceMap("id").toString,
        ApplicationUrl.get + sourceMap("images").asInstanceOf[util.HashMap[String, AnyRef]].get("small").asInstanceOf[util.HashMap[String, String]].get("url"),
        ApplicationUrl.get + sourceMap("id").toString,
        sourceMap("copyright").asInstanceOf[util.HashMap[String, AnyRef]].get("license").asInstanceOf[util.HashMap[String, String]].get("license"))
    }
  }


  override def matchingQuery(query: Iterable[String], minimumSize:Option[Int], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[ImageMetaSummary] = {
    val titleSearch = new ListBuffer[QueryDefinition]
    titleSearch += matchQuery("titles.title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => titleSearch += termQuery("titles.language", lang))

    val altTextSearch = new ListBuffer[QueryDefinition]
    altTextSearch += matchQuery("alttexts.alttext", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => altTextSearch += termQuery("alttexts.language", lang))

    val tagSearch = new ListBuffer[QueryDefinition]
    tagSearch += matchQuery("tags.tag", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => tagSearch += termQuery("tags.language", lang))

    val filterList = new ListBuffer[QueryDefinition]()
    license.foreach(license => filterList += nestedQuery("copyright.license").query(termQuery("copyright.license.license", license)))
    minimumSize.foreach(size => filterList += nestedQuery("images.full").query(rangeQuery("images.full.size").gte(size.toString)))
    filterList += noCopyrightFilter

    val theSearch = search in ImageApiProperties.SearchIndex -> ImageApiProperties.SearchDocument query {
      bool {
        must (
          should (
            nestedQuery("titles").query {bool {must (titleSearch.toList)}},
            nestedQuery("alttexts").query {bool {must (altTextSearch.toList)}},
            nestedQuery("tags").query {bool {must (tagSearch.toList)}}
          ),
          filter (filterList)
        )
      }
    }

    val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
    executeSearch(theSearch, page, pageSize)
  }

  override def all(minimumSize:Option[Int], license: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[ImageMetaSummary] = {
    val filterList = new ListBuffer[QueryDefinition]()
    license.foreach(license => filterList += nestedQuery("copyright.license").query(termQuery("copyright.license.license", license)))
    minimumSize.foreach(size => filterList += nestedQuery("images.full").query(rangeQuery("images.full.size").gte(size.toString)))
    filterList += noCopyrightFilter

    val theSearch = search in ImageApiProperties.SearchIndex -> ImageApiProperties.SearchDocument query filter(filterList)
    theSearch.sort(field sort "id")

    executeSearch(theSearch, page, pageSize)
  }

  def executeSearch(search: SearchDefinition, page: Option[Int], pageSize: Option[Int]) = {
    val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
    try{
      client.execute{search start startAt limit numResults}.await.as[ImageMetaSummary]
    } catch {
      case e:RemoteTransportException => errorHandler(e.getCause)
    }
  }

  def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
    val numResults = pageSize match {
      case Some(num) =>
        if(num > 0) num.min(ImageApiProperties.MaxPageSize) else ImageApiProperties.DefaultPageSize
      case None => ImageApiProperties.DefaultPageSize
    }

    val startAt = page match {
      case Some(sa) => (sa - 1).max(0) * numResults
      case None => 0
    }

    (startAt, numResults)
  }

  def errorHandler(exception: Throwable) = {
    exception match {
      case ex: IndexNotFoundException =>
        logger.error(ex.getDetailedMessage)
        scheduleIndexDocuments()
        throw ex
      case _ => throw exception
    }
  }

  def scheduleIndexDocuments() = {
    val f = Future {
      SearchIndexer.indexDocuments()
    }
    f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
  }
}
