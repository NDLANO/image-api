/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.repository

import java.text.SimpleDateFormat
import java.util.Calendar

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.service.ElasticContentIndexComponent

trait SearchIndexerComponent {
  this: ImageRepositoryComponent with ElasticContentIndexComponent =>
  val searchIndexer: SearchIndexer

  class SearchIndexer extends LazyLogging {

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }

    def indexDocuments() = {
      synchronized {
        val start = System.currentTimeMillis()

        val newIndexName = ImageApiProperties.SearchIndex + "_" + getTimestamp
        val oldIndexName = elasticContentIndex.aliasTarget

        elasticContentIndex.createIndex(newIndexName)

        oldIndexName match {
          case None => elasticContentIndex.updateAliasTarget(newIndexName, oldIndexName)
          case Some(_) =>
        }

        logger.info(s"Indexing all documents into index $newIndexName")

        imageRepository.applyToAll(docs => {
          elasticContentIndex.indexDocuments(docs, newIndexName)
          logger.info(s"Completed indexing of ${docs.size} documents")
        })
        elasticContentIndex.updateAliasTarget(newIndexName, oldIndexName)

        oldIndexName.foreach(indexName => {
          elasticContentIndex.updateAliasTarget(newIndexName, oldIndexName)
          elasticContentIndex.deleteIndex(indexName)
        })

        val result = s"Completed indexing '${ImageApiProperties.SearchIndex}' in ${System.currentTimeMillis() - start} ms."
        logger.info(result)
        result
      }
    }
  }
}