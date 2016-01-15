package no.ndla.imageapi.business

import java.text.SimpleDateFormat
import java.util.Calendar

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.AmazonIntegration


object SearchIndexer extends LazyLogging {

  val meta = AmazonIntegration.getImageMeta()
  val indexMeta = AmazonIntegration.getIndexMeta()

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
}
