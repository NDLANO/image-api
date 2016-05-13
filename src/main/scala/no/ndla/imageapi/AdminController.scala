package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.business.{ImageMeta, SearchIndexer}
import no.ndla.imageapi.integration.AmazonIntegration
import no.ndla.imageapi.model.Error
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.network.ApplicationUrl
import no.ndla.logging.LoggerContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging  {

  protected implicit override val jsonFormats: Formats = DefaultFormats
  val imageMeta: ImageMeta = AmazonIntegration.getImageMeta()

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  // Clear application url and correlationId.
  after() {
    LoggerContext.clearCorrelationID
    ApplicationUrl.clear
  }

  error{
    case t:Throwable => {
      val error = Error(Error.GENERIC, t.getMessage)
      logger.error(error.toString, t)
      halt(status = 500, body = error)
    }
  }

  post("/index") {
    Ok(SearchIndexer.indexDocuments())
  }

  get("/extern/:image_id") {
    val imageId = params("image_id")
    logger.info("GET /extern/{}", imageId)

    if(imageId.forall(_.isDigit)) {
      imageMeta.withExternalId(imageId) match {
        case Some(image) => image
        case None => halt(status = 404, body = Error(NOT_FOUND, s"Image with external id $imageId not found"))
      }
    } else {
      halt(status = 404, body = Error(NOT_FOUND, s"Image with external id $imageId not found"))
    }
  }
}
