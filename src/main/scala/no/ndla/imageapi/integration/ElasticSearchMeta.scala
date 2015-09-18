/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.integration

import java.util

import no.ndla.imageapi.business.SearchMeta
import no.ndla.imageapi.model.ImageMetaSummary

import com.sksamuel.elastic4s.{RichSearchHit, HitAs, ElasticsearchClientUri, ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.index.query.{SimpleQueryStringBuilder, MatchQueryBuilder}
import org.elasticsearch.common.settings.ImmutableSettings

class ElasticSearchMeta(clusterName:String, clusterHost:String, clusterPort:String) extends SearchMeta {

  val UrlPrefix = "http://api.test.ndla.no/images/"

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
    val response = client.execute {
      search in "images" -> "image" query(tagList.mkString(" ")).defaultOperator(SimpleQueryStringBuilder.Operator.AND) limit 500
    }.await

    response.as[ImageMetaSummary]

  }
}
