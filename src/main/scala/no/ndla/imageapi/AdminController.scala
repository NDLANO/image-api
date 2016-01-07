package no.ndla.imageapi

import java.text.SimpleDateFormat
import java.util.Calendar

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration.AmazonIntegration
import no.ndla.imageapi.model.Error
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.NativeJsonSupport

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging  {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  val meta = AmazonIntegration.getImageMeta()
  val indexMeta = AmazonIntegration.getIndexMeta()

  error{
    case t:Throwable => {
      val error = Error(Error.GENERIC, t.getMessage)
      logger.error(error.toString, t)
      halt(status = 500, body = error)
    }
  }

  def getTimestamp: String = {
    new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
  }

  def indexDocuments() = {
    synchronized {
      val start = System.currentTimeMillis()

      val newIndexName = ImageApiProperties.SearchIndex + "_" + getTimestamp
      val oldIndexName = indexMeta.aliasTarget

      indexMeta.createIndex(newIndexName)

      oldIndexName match {
        case None => indexMeta.updateAliasTarget(newIndexName, oldIndexName)
        case Some(_) =>
      }

      logger.info(s"Indexing all documents into index $newIndexName")

      meta.applyToAll(docs => {
        indexMeta.indexDocuments(docs, newIndexName)
        logger.info(s"Completed indexing of ${docs.size} documents")
      })
      indexMeta.updateAliasTarget(newIndexName, oldIndexName)

      oldIndexName.foreach(indexName => {
        indexMeta.updateAliasTarget(newIndexName, oldIndexName)
        indexMeta.deleteIndex(indexName)
      })

      val result = s"Completed indexing '${ImageApiProperties.SearchIndex}' in ${System.currentTimeMillis() - start} ms."
      logger.info(result)
      result
    }
  }

  post("/index") {
    Ok(indexDocuments())
  }
}
