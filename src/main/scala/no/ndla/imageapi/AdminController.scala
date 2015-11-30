package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration.AmazonIntegration
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, Ok, ScalatraServlet}
import org.scalatra.json.NativeJsonSupport

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging  {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  val meta = AmazonIntegration.getImageMeta()
  val search = AmazonIntegration.getSearchMeta()
  val indexMeta = AmazonIntegration.getIndexMeta()

  def indexDocuments() = {
    val start = System.currentTimeMillis()

    val prevIndex = indexMeta.usedIndex
    val index = prevIndex + 1
    logger.info(s"Indexing all documents into index $index")
    indexMeta.createIndex(index)
    meta.applyToAll(docs => {
      indexMeta.indexDocuments(docs, index)
    })
    indexMeta.useIndex(index)
    if(prevIndex > 0) {
      indexMeta.deleteIndex(prevIndex)
    }
    
    val result = s"Indexing took ${System.currentTimeMillis() - start} ms."
    logger.info(result)
  }

  post("/index") {
    try {
      indexDocuments()
      Ok()
    } catch {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }
}
