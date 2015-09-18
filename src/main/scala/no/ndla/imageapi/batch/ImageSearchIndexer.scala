package no.ndla.imageapi.batch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.mappings.FieldType._
import org.elasticsearch.common.settings.ImmutableSettings
import org.json4s.native.JsonMethods._

import scalaj.http.Http

object ImageSearchIndexer {

  val IndexName = "images"
  val IndexHost = "52.28.51.79"
  val IndexPort = 9300
  val ClusterName = "image-search"
  val DocumentName = "image"

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", ClusterName).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(s"elasticsearch://$IndexHost:$IndexPort"))

  def main(args: Array[String]) {
    createIndex()
    indexDocuments()
  }

  def indexDocuments() = {
    implicit val formats = org.json4s.DefaultFormats

    (1 to 1037).foreach(id => {
      val getResponse = Http(s"http://api.test.ndla.no/images/$id").asString
      val jsonString = getResponse.body
      val json = parse(jsonString)
      val documentId = (json \ "id").extract[String]

      client.execute{
        index into IndexName -> DocumentName source jsonString id documentId
      }.await

      println(s"Indexed document with id: $documentId.")
    })

  }

  def createIndex() = {
    val existsDefinition = client.execute{
      index exists IndexName
    }.await

    if(!existsDefinition.isExists){
      client.execute {
        create index IndexName mappings(
          DocumentName as (
            "titles" typed NestedType as (
              "title" typed StringType,
              "language" typed StringType index "not_analyzed"
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

