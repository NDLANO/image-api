package no.ndla.imageapi

import no.ndla.imageapi.controller.NdlaController
import no.ndla.imageapi.integration.MappingApiClient
import no.ndla.imageapi.model.Error
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.repository.ImageRepositoryComponent
import no.ndla.imageapi.service.{ConverterService, ImportServiceComponent}
import org.scalatra.Ok

import scala.util.{Failure, Success}

trait InternController {
  this: ImageRepositoryComponent with ImportServiceComponent with ConverterService with MappingApiClient =>
  val internController: InternController

  class InternController extends NdlaController {

    post("/index") {
      Ok(ComponentRegistry.searchIndexer.indexDocuments())
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
