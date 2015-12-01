package no.ndla.imageapi

import java.text.SimpleDateFormat
import java.util.Calendar

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

  def getTimestamp: String = {
    val timestampFormat = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss")
    timestampFormat.format(Calendar.getInstance.getTime)
  }

  def indexDocuments() = {
    val start = System.currentTimeMillis()

    val prevIndex = indexMeta.indexInUse
    val indexName = ImageApiProperties.SearchIndex + "_" + getTimestamp
    logger.info(s"Indexing all documents into index $indexName")
    indexMeta.createIndex(indexName)
    meta.applyToAll(docs => {
      indexMeta.indexDocuments(docs, indexName)
    })
    indexMeta.useIndex(indexName)
    prevIndex.foreach(prevIndexName => {
      indexMeta.deleteIndex(prevIndexName)
    })

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
