/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model.S3UploadException
import no.ndla.imageapi.model.api.Error
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{IndexBuilderService, IndexService}
import no.ndla.imageapi.service.{ConverterService, ImportService, ReadService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{BadRequest, GatewayTimeout, InternalServerError, NotFound, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: ImageRepository
    with ReadService
    with ImportService
    with ConverterService
    with IndexBuilderService
    with IndexService
    with User =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      indexBuilderService.indexDocuments match {
        case Success(reindexResult) => {
          val result =
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    delete("/index") {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      val deleteResults = indexService.findAllIndexes(ImageApiProperties.SearchIndex) match {
        case Failure(f) => halt(status = 500, body = f.getMessage)
        case Success(indexes) =>
          indexes.map(index => {
            logger.info(s"Deleting index $index")
            indexService.deleteIndexWithName(Option(index))
          })
      }
      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
      }
    }

    get("/extern/:image_id") {
      val externalId = params("image_id")
      val language = paramOrNone("language")
      imageRepository.withExternalId(externalId) match {
        case Some(image) => Ok(converterService.asApiImageMetaInformationWithDomainUrlV2(image, language))
        case None        => NotFound(Error(Error.NOT_FOUND, s"Image with external id $externalId not found"))
      }
    }

    get("/id_from_url/") {
      val urlQueryParam = "url"
      val url = paramOrNone(urlQueryParam)
      url match {
        case Some(p) =>
          readService.getDomainImageMetaFromUrl(p) match {
            case Success(image) => Ok(image)
            case Failure(ex)    => errorHandler(ex)
          }
        case None =>
          BadRequest(Error(Error.VALIDATION, s"Query param '$urlQueryParam' needs to be specified to return an id"))
      }
    }

    post("/import/:image_id") {
      authUser.assertHasId()
      val start = System.currentTimeMillis
      val imageId = params("image_id")

      importService.importImage(imageId) match {
        case Success(imageMeta) => {
          Ok(converterService.asApiImageMetaInformationWithDomainUrlV2(imageMeta, None))
        }
        case Failure(s: S3UploadException) => {
          val errMsg =
            s"Import of node with external_id $imageId failed after ${System.currentTimeMillis - start} ms with error: ${s.getMessage}\n"
          logger.warn(errMsg, s)
          GatewayTimeout(body = Error(Error.GATEWAY_TIMEOUT, errMsg))
        }
        case Failure(ex: Throwable) => {
          val errMsg =
            s"Import of node with external_id $imageId failed after ${System.currentTimeMillis - start} ms with error: ${ex.getMessage}\n"
          logger.warn(errMsg, ex)
          InternalServerError(body = errMsg)
        }
      }
    }
  }

}
