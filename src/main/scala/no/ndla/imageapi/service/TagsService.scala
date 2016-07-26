/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.service

import no.ndla.imageapi.integration.MappingApiClient
import no.ndla.imageapi.model.domain.ImageTag

import scala.io.Source
import scala.util.matching.Regex

trait TagsService {
  this: MappingApiClient =>
  val tagsService: TagsService

  class TagsService {

    val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
    val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

    def forImage(nid: String): List[ImageTag] = {
      import org.json4s.native.JsonMethods._
      import org.json4s.native.Serialization.read
      implicit val formats = org.json4s.DefaultFormats

      val jsonString = Source.fromURL(TopicAPIUrl + nid).mkString
      val json = parse(jsonString)

      read[Keywords](jsonString)
        .keyword
        .flatMap(_.names)
        .flatMap(_.data)
        .flatMap(_.toIterable)
        .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
        .groupBy(_._1).map(entry => (entry._1, entry._2.map(_._2)))
        .map(t => ImageTag(t._2, t._1)).toList
    }

    def getISO639(languageUrl: String): Option[String] = {
      Option(languageUrl) collect { case pattern(group) => group } match {
        case Some(x) => if (x == "language-neutral") None else mappingApiClient.get6391CodeFor6392Code(x)
        case None => None
      }
    }
  }


}

case class Keywords(keyword: List[Keyword])

case class Keyword(psi: Option[String], topicId: Option[String], visibility: Option[String], approved: Option[String], processState: Option[String], psis: List[String],
                   originatingSites: List[String], types: List[Any], names: List[KeywordName])

case class Type(typeId: String)

case class TypeName(isoLanguageCode: String)

case class KeywordName(wordclass: String, data: List[Map[String, String]])

case class KeywordNameName(isoLanguageCode: String)
