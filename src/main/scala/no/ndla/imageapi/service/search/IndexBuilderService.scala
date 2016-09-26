/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.repository.ImageRepository

trait IndexBuilderService {
  this: ImageRepository with IndexService =>
  val indexBuilderService: IndexBuilderService

  class IndexBuilderService extends LazyLogging {

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }

    def buildIndex() = {
      synchronized {
        val start = System.currentTimeMillis()

        val newIndexName = ImageApiProperties.SearchIndex + "_" + getTimestamp
        val oldIndexName = indexService.aliasTarget

        indexService.createIndex(newIndexName)

        oldIndexName match {
          case None => indexService.updateAliasTarget(newIndexName, oldIndexName)
          case Some(_) =>
        }

        logger.info(s"Indexing all documents into index $newIndexName")

        var numIndexed = 0
        imageRepository.applyToAll(docs => {
          numIndexed += indexService.indexDocuments(docs, newIndexName)
          logger.info(s"Completed indexing of $numIndexed documents")
        })

        oldIndexName.foreach(indexName => {
          indexService.updateAliasTarget(newIndexName, oldIndexName)
          indexService.deleteIndex(indexName)
        })

        val result = s"Completed indexing $numIndexed documents into '${ImageApiProperties.SearchIndex}' in ${System.currentTimeMillis() - start} ms."
        logger.info(result)
        result
      }
    }
  }
}