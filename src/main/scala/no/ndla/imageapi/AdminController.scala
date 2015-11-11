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

  def indexDocuments() = {
    val start = System.currentTimeMillis()
    val index = (search.usedIndex + 1)
    logger.info(s"Indexing ${meta.elements.length} documents into index $index")
    search.createIndex(index)
    search.indexDocuments(meta.elements, index)
    search.useIndex(index)
    val result = s"Indexing took ${System.currentTimeMillis() - start} ms."
    logger.info(result)
    s"$result\nnew alias: ${search.IndexName} -> $index\n"
  }

  post("/index") {
    indexDocuments()
    /*
    val indexName = params.get("indexName")
    indexName match {
      case None => "please specify index name.\n"
      case Some(search.IndexName) => s"index name cannot be the same as alias name (${search.IndexName})\n"
      case Some(search.usedIndex) => s"Please specify unique index name (Index $indexName already in use)."
      case Some(idx) => indexDocuments(idx)
    }
    */
  }
}
