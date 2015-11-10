package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration.AmazonIntegration
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.NativeJsonSupport

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging  {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  val meta = AmazonIntegration.getImageMeta()
  val search = AmazonIntegration.getSearchMeta()

  def indexDocuments(indexName: String) = {
    val start = System.currentTimeMillis()
    search.createIndex(indexName)
    meta.foreach(imageMeta => {
      search.indexDocument(imageMeta, indexName)
      println(s"Indexed document with id ${imageMeta.id} into index ${indexName}.")
    })
    search.useIndex(indexName)
    val result = s"Indexing took ${System.currentTimeMillis() - start} ms."
    println(result)
    result + "\n" + s"new alias: ${search.IndexName} -> ${indexName}\n"
  }

  post("/index") {
    val indexName = params.get("indexName")
    indexName match {
      case None => "please specify index name.\n"
      case Some(search.IndexName) => s"index name cannot be the same as alias name (${search.IndexName})\n"
      case Some(idx) => indexDocuments(idx)
    }
  }
}
