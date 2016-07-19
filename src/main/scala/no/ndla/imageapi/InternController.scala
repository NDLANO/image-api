package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.Error
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.network.ApplicationUrl
import no.ndla.imageapi.repository.ImageRepositoryComponent
import no.ndla.imageapi.service.{ConverterService, ImportServiceComponent}
import no.ndla.logging.LoggerContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

import scala.util.{Failure, Success}

trait InternController {
  this: ImageRepositoryComponent with ImportServiceComponent with ConverterService =>
  val internController: InternController

  class InternController extends ScalatraServlet with NativeJsonSupport with LazyLogging {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    before() {
      contentType = formats("json")
      LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
      ApplicationUrl.set(request)
      logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
    }

    after() {
      LoggerContext.clearCorrelationID
      ApplicationUrl.clear
    }

    error {
      case t: Throwable => {
        val error = Error(Error.GENERIC, t.getMessage)
        logger.error(error.toString, t)
        halt(status = 500, body = error)
      }
    }

    post("/index") {
      Ok(ComponentRegistry.searchIndexer.indexDocuments())
    }

    get("/extern/:image_id") {
      val externalId = params("image_id")

      if (externalId.forall(_.isDigit)) {
        imageRepository.withExternalId(externalId) match {
          case Some(image) => converterService.asApiImageMetaInformationWithDomainUrl(image)
          case None => halt(status = 404, body = Error(NOT_FOUND, s"Image with external id $externalId not found"))
        }
      } else {
        halt(status = 404, body = Error(NOT_FOUND, s"Image with external id $externalId not found"))
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
