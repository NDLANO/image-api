package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.business.{ImageMeta, ImageStorage}
import no.ndla.imageapi.integration.AmazonIntegration
import no.ndla.imageapi.model.{Error, ImageMetaInformation, ImageMetaSummary}
import no.ndla.imageapi.model.Error._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json._
import org.scalatra.swagger.{Swagger, SwaggerSupport}

class ImageController (implicit val swagger:Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport with LazyLogging {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  // Swagger-stuff
  protected val applicationDescription = "API for accessing images from ndla.no."

  val getImages =
    (apiOperation[List[ImageMetaSummary]]("getImages")
      summary "Show all images"
      notes "Shows all the images in the ndla.no database. You can search it too."
      parameters (
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access."),
        queryParam[Option[String]]("tags").description("Return only images with submitted tag. Multiple tags may be entered comma separated, and will give results matching either one of them."),
        queryParam[Option[String]]("minimumSize").description("Return only images with full size larger than submitted value in bytes."),
        queryParam[Option[String]]("lang").description("The ISO 639-1 language code describing language used in query-params.")
      ))

  val getByImageId =
    (apiOperation[ImageMetaInformation]("findByImageId")
      summary "Show image info"
      notes "Shows info of the image with submitted id."
      parameter pathParam[String]("image_id").description("Image_id of the image that needs to be fetched."))


  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  val imageMeta: ImageMeta = AmazonIntegration.getImageMeta()
  val imageStorage: ImageStorage = AmazonIntegration.getImageStorage()


  get("/", operation(getImages)) {
    val size = params.get("minimumSize") match {
      case Some(size) => if (size.forall(_.isDigit)) Option(size.toInt) else None
      case None => None
    }

    params.get("tags") match {
      case Some(tags) => imageMeta.withTags(
        tags = tags.toLowerCase().split(",").map(_.trim),
        minimumSize = size)

      case None => imageMeta.all(minimumSize = size)
    }
  }

  get("/:image_id", operation(getByImageId)) {
    if(params("image_id").forall(_.isDigit)) {
      imageMeta.withId(params("image_id")) match {
        case Some(image) => image
        case None => halt(status = 404, body = Error(NOT_FOUND, "Image with id " + params("image_id") + " not found"))
      }
    } else {
      halt(status = 404, body = Error(NOT_FOUND, "Image with id " + params("image_id") + " not found"))
    }
  }

  get("/thumbs/:name") {
    imageStorage.get("thumbs/" + params("name")) match {
      case Some(image) => {
        contentType = image._1
        image._2
      }
      case None => halt(status = 404, body = Error(NOT_FOUND, "Image with key /thumbs/" + params("name") + " not found"))
    }
  }

  get("/full/:name") {
    imageStorage.get("full/" + params("name")) match {
      case Some(image) => {
        contentType = image._1
        image._2
      }
      case None => halt(status = 404, body = Error(NOT_FOUND, "Image with key /full/" + params("name") + " not found"))
    }
  }

  error{
    case t:Throwable => {
      val error = Error(GENERIC, "Internal error occured")
      logger.error(error.toString, t)

      halt(status = 500, body = error)
    }
  }
}
