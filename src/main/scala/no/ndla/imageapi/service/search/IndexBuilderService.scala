/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.domain.{ImageMetaInformation, ReindexResult}
import no.ndla.imageapi.repository.ImageRepository

import scala.util.{Failure, Success, Try}

trait IndexBuilderService {
  this: ImageRepository with IndexService =>
  val indexBuilderService: IndexBuilderService

  class IndexBuilderService extends LazyLogging {

    def indexDocument(imported: ImageMetaInformation): Try[ImageMetaInformation] = {
      for {
        _ <- indexService.aliasTarget.map {
          case Some(index) => Success(index)
          case None => indexService.createIndexWithGeneratedName().map(newIndex => indexService.updateAliasTarget(None, newIndex))
        }
        imported <- indexService.indexDocument(imported)
      } yield imported
    }

    def createEmptyIndex: Try[Option[String]] = {
      indexService.createIndexWithGeneratedName().flatMap(indexName => {
        for {
          aliasTarget <- indexService.aliasTarget
          _ <- indexService.updateAliasTarget(aliasTarget, indexName)
          _ <- indexService.deleteIndex(aliasTarget)
        } yield (aliasTarget)
      })
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        indexService.createIndexWithGeneratedName().flatMap(indexName => {
          val operations = for {
            numIndexed <- sendToElastic(indexName)
            aliasTarget <- indexService.aliasTarget
            _ <- indexService.updateAliasTarget(aliasTarget, indexName)
            _ <- indexService.deleteIndex(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              indexService.deleteIndex(Some(indexName))
              Failure(f)
            }
            case Success(totalIndexed) => {
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
            }
          }
        })
      }
    }

    def sendToElastic(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = indexService.indexDocuments(imageRepository.imagesWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f) => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges:Try[List[(Long,Long)]] = {
      Try{
        val (minId, maxId) = imageRepository.minMaxId
        Seq.range(minId, maxId + 1).grouped(ImageApiProperties.IndexBulkSize).map(group => (group.head, group.last)).toList
      }
    }
  }
}