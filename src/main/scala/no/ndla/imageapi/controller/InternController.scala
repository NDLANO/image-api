/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ComponentRegistry
import no.ndla.imageapi.integration.MappingApiClient
import no.ndla.imageapi.model.Error
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.{ConverterService, ImportService}
import org.scalatra.Ok

import scala.util.{Failure, Success}

trait InternController {
  this: ImageRepository with ImportService with ConverterService with MappingApiClient =>
  val internController: InternController

  class InternController extends NdlaController {

    post("/index") {
      Ok(ComponentRegistry.indexBuilderService.buildIndex())
    }

    get("/extern/:image_id") {
      val externalId = params("image_id")
      imageRepository.withExternalId(externalId) match {
        case Some(image) => converterService.asApiImageMetaInformationWithDomainUrl(image)
        case None => halt(status = 404, body = Error(NOT_FOUND, s"Image with external id $externalId not found"))
      }
    }

    post("/import/:image_id") {
      val start = System.currentTimeMillis
      val imageId = params("image_id")

      importService.importImage(imageId) match {
        case Success(imageMeta) => converterService.asApiImageMetaInformationWithDomainUrl(imageMeta)
        case Failure(ex: Throwable) => {
          val errMsg = s"Import of node with external_id $imageId failed after ${System.currentTimeMillis - start} ms with error: ${ex.getMessage}\n"
          logger.warn(errMsg, ex)
          halt(status = 500, body = errMsg)
        }
      }
    }
  }

}
