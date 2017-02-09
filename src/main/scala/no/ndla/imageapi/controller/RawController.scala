package no.ndla.imageapi.controller

import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.model.api.ImageMetaInformation
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.service.{ImageConverter, ImageStorageService}
import org.scalatra.swagger.{Swagger, SwaggerSupport}

import scala.util.{Failure, Success, Try}

trait RawController {
  this: ImageStorageService with ImageConverter =>
  val rawController: RawController

  class RawController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected val applicationDescription = "API for accessing image files from ndla.no."

    val getImageFile =
      (apiOperation[ImageMetaInformation]("getImageFile")
        summary "Fetches a raw image"
        notes "Fetches an image with options to resize and crop"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("name").description("The name of the image"),
        queryParam[Option[Int]]("width").description("The target width to resize the image. Image proportions are kept intact"),
        queryParam[Option[Int]]("height").description("The target height to resize the image. Image proportions are kept intact"),
        queryParam[Option[String]]("cropStart").description(
          """The first image coordinate (X,Y) specifying the crop start position.
            |The coordinate is a comma separated value of the form row,column (e.g 100,10).
            |If cropStart is specified cropEnd must also be specified""".stripMargin),
        queryParam[Option[String]]("cropEnd").description(
          """The second image coordinate (X,Y) specifying the crop end position, forming a square cutout.
            |The coordinate is a comma separated value of the form row,column (e.g 200,100).
            |If cropEnd is specified cropStart must also be specified""".stripMargin)
      ))

    get("/:name", operation(getImageFile)) {
      val imageName = params("name")
      logger.info(s"Fetching '$imageName'")
      imageStorage.get(imageName).flatMap(crop).flatMap(resize) match {
        case Success(img) => img
        case Failure(e) => errorHandler(new ImageNotFoundException(s"image $imageName as not found"))
      }
    }

    def crop(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      (paramAsListOfInt("cropStart"), paramAsListOfInt("cropEnd")) match {
        case (List(x1, y1), List(x2, y2)) => imageConverter.crop(image, CropOptions(x1, y1, x2, y2))
        case _ => Success(image)
      }
    }

    def resize(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      extractIntOpts("width", "height") match {
        case Seq(Some(width), _) => imageConverter.resize(image, width)
        case Seq(_, Some(height)) => imageConverter.resize(image, height)
        case Seq(Some(width), Some(height)) => imageConverter.resize(image, width, height)
        case _ => Success(image)
      }
    }

  }
}
