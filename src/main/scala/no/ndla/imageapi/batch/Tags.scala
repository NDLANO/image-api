package no.ndla.imageapi.batch

import no.ndla.imageapi.model.ImageTag

import scala.io.Source
import scala.util.matching.Regex

object Tags {

  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"
  val pattern = new Regex("http:\\/\\/psi\\..*\\/#(.+)")

  def main(args: Array[String]) {
    val tags = forImage("145741")
    tags.foreach(println)

  }

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
      .map(t => ImageTag(t._2.trim.toLowerCase, getISO639(t._1)))
  }

  def getISO639(languageUrl:String): Option[String] = {
    Option(languageUrl) collect { case pattern(group) => group } match {
      case Some(x) => if (x == "language-neutral") None else Some(x)
      case None => None
    }
  }
}

case class Keywords(keyword: List[Keyword])
case class Keyword(psi: String, topicId: String, visibility: String, approved: String, processState: String, psis: List[String],
                   originatingSites: List[String], types: List[Any], names: List[KeywordName])

case class Type(typeId:String)
case class TypeName(isoLanguageCode: String)

case class KeywordName(wordclass: String, data: List[Map[String,String]])
case class KeywordNameName(isoLanguageCode: String)

