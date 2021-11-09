/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.Language
import no.ndla.imageapi.model.domain.ImageTag
import no.ndla.mapping.ISO639.get6391CodeFor6392Code
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.read

import java.io.InputStream
import scala.io.Source
import scala.util.matching.Regex

trait TagsService {
  val tagsService: TagsService

  class TagsService {

    val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

    def streamToImageTags(stream: InputStream): Seq[ImageTag] = {
      implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
      read[Keywords](Source.fromInputStream(stream).mkString).keyword
        .flatMap(_.names)
        .flatMap(_.data)
        .flatMap(_.toIterable)
        .map(t => (getISO639(t._1), t._2.trim.toLowerCase))
        .groupBy(_._1)
        .map(entry => (entry._1, entry._2.map(_._2)))
        .map(t => ImageTag(t._2, Language.languageOrUnknown(t._1).toString()))
        .toList
    }

    def getISO639(languageUrl: String): Option[String] = {
      Option(languageUrl) collect { case pattern(group) => group } match {
        case Some(x) => if (x == "language-neutral") None else get6391CodeFor6392Code(x)
        case None    => None
      }
    }
  }

}

case class Keywords(keyword: List[Keyword])

case class Keyword(psi: Option[String],
                   topicId: Option[String],
                   visibility: Option[String],
                   approved: Option[String],
                   processState: Option[String],
                   psis: List[String],
                   originatingSites: List[String],
                   types: List[Any],
                   names: List[KeywordName])

case class Type(typeId: String)

case class TypeName(isoLanguageCode: String)

case class KeywordName(wordclass: String, data: List[Map[String, String]])

case class KeywordNameName(isoLanguageCode: String)
