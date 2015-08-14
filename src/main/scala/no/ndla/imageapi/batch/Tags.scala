package no.ndla.imageapi.batch

import scala.io.Source

object Tags {

  val TopicAPIUrl = "http://api.topic.ndla.no/rest/v1/keywords/?filter[node]=ndlanode_"

  def forImage(nid: String): List[String] = {
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
      .filter(_._1 == "http://psi.oasis-open.org/iso/639/#nob")
      .map(_._2)
  }
}

case class Keywords(keyword: List[Keyword])
case class Keyword(psi: String, topicId: String, visibility: String, approved: String, processState: String, psis: List[String],
                   originatingSites: List[String], types: List[Any], names: List[KeywordName])

case class Type(typeId:String)
case class TypeName(isoLanguageCode: String)

case class KeywordName(wordclass: String, data: List[Map[String,String]])
case class KeywordNameName(isoLanguageCode: String)

