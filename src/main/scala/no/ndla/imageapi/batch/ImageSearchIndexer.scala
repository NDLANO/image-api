package no.ndla.imageapi.batch

import no.ndla.imageapi.ImageApiProperties
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import scalaj.http._

/**
 * How to delete the index:
 * curl -XDELETE 'http://localhost:9200/images/'
 */
object ImageSearchIndexer {

  val IndexName = "images"
  val IndexHost = "localhost"
  val IndexPort = 9200
  val ClusterName = "image-search"
  val DocumentName = "image"


  implicit val formats = org.json4s.DefaultFormats

  def main (args: Array[String]) {
    if(serverAlive && createIndex && createMapping) {
      indexDocuments()
    }
  }


  def indexDocuments() = {
    //(1 to 1037).foreach(id => {
    (1 to 1037).foreach(id => {
      val getResponse = Http(s"http://api.test.ndla.no/images/$id").asString
      val jsonString = getResponse.body
      val json = parse(jsonString)
      val documentId = (json \ "id").extract[String]

      val putResponse = Http(s"http://$IndexHost:$IndexPort/$IndexName/$DocumentName/$documentId").method("put").postData(jsonString).asString
      val putBody = putResponse.body
      val putJson = parse(putBody)
      val status = (putJson \ "created").extract[Boolean]

      println(s"Tried to index document with id: $documentId. Successful: $status")

    })
  }

  def createMapping():Boolean = {
    val mapping = io.Source.fromInputStream(getClass.getResourceAsStream("/imagemapping.json")).mkString
    val response = Http(s"http://$IndexHost:$IndexPort/$IndexName/_mapping/$DocumentName").method("put").postData(mapping).asString
    val json = parse(response.body)

    (json \ "acknowledged").extract[Boolean]
  }

  def createIndex() = {
    indexExists() || {
      val response: HttpResponse[String] = Http(s"http://$IndexHost:$IndexPort/$IndexName").method("put").asString
      val json = parse(response.body)

      (json \ "acknowledged").extract[Boolean]
    }
  }

  def indexExists(): Boolean = {
    val responseCode = Http(s"http://$IndexHost:$IndexPort/$IndexName/").method("head").asString.code
    responseCode == 200
  }

  def serverAlive(): Boolean = {
    val response: HttpResponse[String] = Http(s"http://$IndexHost:$IndexPort/_cluster/health").asString

    val json = parse(response.body)
    val status = (json \ "status").extract[String]
    val clusterName = (json \ "cluster_name").extract[String]

    // TODO: Vi bør nok kjøre clusteret med minst to noder.
    (status == "green" || status == "yellow") && clusterName == ClusterName

  }

}

