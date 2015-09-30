/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.integration

import java.util

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.business.SearchMeta
import no.ndla.imageapi.model.{ImageMetaInformation, ImageMetaSummary}

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import org.elasticsearch.index.query.{SimpleQueryStringBuilder, MatchQueryBuilder}
import org.elasticsearch.common.settings.ImmutableSettings

import scala.collection.mutable.ListBuffer

class ElasticSearchMeta(clusterName:String, clusterHost:String, clusterPort:String) extends SearchMeta with LazyLogging {

  val IndexName = "images"
  val DocumentName = "image"

  val UrlPrefix = "http://api.test.ndla.no/images/"

  val PageSize = 100
  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(s"elasticsearch://$clusterHost:$clusterPort"))

  implicit object ImageHitAs extends HitAs[ImageMetaSummary] {
    override def as(hit: RichSearchHit): ImageMetaSummary = {
      val sourceMap = hit.sourceAsMap
      ImageMetaSummary(
        sourceMap("id").toString,
        sourceMap("images").asInstanceOf[util.HashMap[String, AnyRef]].get("small").asInstanceOf[util.HashMap[String, String]].get("url"),
        UrlPrefix + sourceMap("id").toString,
        sourceMap("copyright").asInstanceOf[util.HashMap[String, AnyRef]].get("license").asInstanceOf[util.HashMap[String, String]].get("license"))
    }
  }


  override def withTags(tagList: Iterable[String], minimumSize:Option[Int], language: Option[String], license: Option[String]): Iterable[ImageMetaSummary] = {
    logger.info(s"Using client at host = $clusterHost, port = $clusterPort with clusterName = $clusterName")

    val titleSearch = new ListBuffer[QueryDefinition]
    titleSearch += matchQuery("title", tagList.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => titleSearch += termQuery("language", lang))

    val tagSearch = new ListBuffer[QueryDefinition]
    tagSearch += matchQuery("tag", tagList.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => tagSearch += termQuery("language", lang))

    val theSearch = search in IndexName -> DocumentName query {
      bool {
        should (
          nestedQuery("titles").query {bool {must (titleSearch.toList)}},
          nestedQuery("tags").query {bool {must (tagSearch.toList)}}
        )
      }
    }

    val filterList = new ListBuffer[FilterDefinition]()
    license.foreach(license => filterList += nestedFilter("copyright.license").filter(termFilter("license", license)))
    minimumSize.foreach(size => filterList += nestedFilter("images.full").filter(rangeFilter("images.full.size").gte(size.toString)))

    if(filterList.nonEmpty){
      theSearch.postFilter(must(filterList.toList))
    }

    client.execute{theSearch limit PageSize}.await.as[ImageMetaSummary]
  }

  override def all(minimumSize:Option[Int], license: Option[String]): Iterable[ImageMetaSummary] = {
    val theSearch = search in IndexName -> DocumentName

    val filterList = new ListBuffer[FilterDefinition]()
    license.foreach(license => filterList += nestedFilter("copyright.license").filter(termFilter("license", license)))
    minimumSize.foreach(size => filterList += nestedFilter("images.full").filter(rangeFilter("images.full.size").gte(size.toString)))

    if(filterList.nonEmpty){
      theSearch.postFilter(must(filterList.toList))
    }
    theSearch.sort(field sort "id")

    client.execute{theSearch limit PageSize}.await.as[ImageMetaSummary]
  }

  override def indexDocument(imageMeta: ImageMetaInformation) = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    client.execute{
      index into IndexName -> DocumentName source write(imageMeta) id imageMeta.id
    }.await
  }

  override def createIndex() = {
    val existsDefinition = client.execute{
      index exists IndexName
    }.await

    if(!existsDefinition.isExists){
      client.execute {
        create index IndexName mappings(
          DocumentName as (
            "id" typed IntegerType,
            "titles" typed NestedType as (
              "title" typed StringType,
              "language" typed StringType index "not_analyzed"
              ),
            "images" typed NestedType as (
              "small" typed NestedType as (
                "url" typed StringType,
                "size" typed IntegerType index "not_analyzed",
                "contentType" typed StringType
                ),
              "full" typed NestedType as (
                "url" typed StringType,
                "size" typed IntegerType index "not_analyzed",
                "contentType" typed StringType
                )
              ),
            "copyright" typed NestedType as (
              "license" typed NestedType as (
                "license" typed StringType index "not_analyzed",
                "description" typed StringType,
                "url" typed StringType
                ),
              "origin" typed StringType,
              "authors" typed NestedType as (
                "type" typed StringType,
                "name" typed StringType
                )
              ),
            "tags" typed NestedType as (
              "tag" typed StringType,
              "language" typed StringType index "not_analyzed"
              )
            )
          )
      }.await
    }
  }
}
