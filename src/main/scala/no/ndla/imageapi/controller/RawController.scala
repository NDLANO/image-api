package no.ndla.imageapi.controller

import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import no.ndla.imageapi.model.api.Error
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.{ImageConverter, ImageStorageService}
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success, Try}

trait RawController {
  this: ImageStorageService with ImageConverter with ImageRepository =>
  val rawController: RawController

  class RawController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected val applicationDescription = "API for accessing image files from ndla.no."

    registerModel[Error]()

    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val getImageFile = new OperationBuilder(ValueDataType("file", Some("binary")))
      .nickname("getImageFile")
      .summary("Fetches a raw image")
      .notes("Fetches a image with options to resize and crop")
      .produces("application/octet-stream")
      .authorizations("oauth2")
      .parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("name").description("The name of the image"),
        queryParam[Option[Int]]("width").description("The target width to resize the image (the unit is pixles). Image proportions are kept intact"),
        queryParam[Option[Int]]("height").description("The target height to resize the image (the unit is pixles). Image proportions are kept intact"),
        queryParam[Option[Int]]("cropStartX").description("The first image coordinate X specifying the crop start position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("cropStartY").description("The first image coordinate Y specifying the crop start position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("cropEndX").description("The end image coordinate X specifying the crop end position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("cropEndY").description("The end image coordinate Y specifying the crop end position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("focalX").description("The end image coordinate X specifying the crop end position (A value from 0 to 100). If used the other focal point parameters, width and/or height, must also be supplied"),
        queryParam[Option[Int]]("focalY").description("The end image coordinate Y specifying the crop end position (A value from 0 to 100). If used the other focal point parameters, width and/or height, must also be supplied"),
      ).responseMessages(response404, response500)

    val getImageFileById = new OperationBuilder(ValueDataType("file", Some("binary")))
      .nickname("getImageFileById")
      .summary("Fetches a raw image using the image id")
      .notes("Fetches a image with options to resize and crop")
      .produces("application/octet-stream")
      .authorizations("oauth2")
      .parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("id").description("The ID of the image"),
        queryParam[Option[Int]]("width").description("The target width to resize the image (the unit is pixles). Image proportions are kept intact"),
        queryParam[Option[Int]]("height").description("The target height to resize the image (the unit is pixles). Image proportions are kept intact"),
        queryParam[Option[Int]]("cropStartX").description("The first image coordinate X specifying the crop start position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("cropStartY").description("The first image coordinate Y specifying the crop start position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("cropEndX").description("The end image coordinate X specifying the crop end position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("cropEndY").description("The end image coordinate Y specifying the crop end position (A value from 0 to 100). If used the other crop parameters must also be supplied"),
        queryParam[Option[Int]]("focalX").description("The end image coordinate X specifying the crop end position (A value from 0 to 100). If used the other focal point parameters, width and/or height, must also be supplied"),
        queryParam[Option[Int]]("focalY").description("The end image coordinate Y specifying the crop end position (A value from 0 to 100). If used the other focal point parameters, width and/or height, must also be supplied"),
      ).responseMessages(response404, response500)

    get("/:name", operation(getImageFile)) {
      getRawImage(params("name"))
    }

    get("/id/:id", operation(getImageFileById)) {
     imageRepository.withId(long("id")) match {
        case Some(imageMeta) => getRawImage(imageMeta.imageUrl)
        case None => None
      }
    }

    private def getRawImage(imageName: String): ImageStream = {
      imageStorage.get(imageName)
        .flatMap(crop)
        .flatMap(dynamicCrop)
        .flatMap(resize) match {
        case Success(img) => img
        case Failure(e) => throw e
      }
    }

    def crop(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      val startX = doubleInRange("cropStartX", PercentPoint.MinValue, PercentPoint.MaxValue)
      val startY = doubleInRange("cropStartY", PercentPoint.MinValue, PercentPoint.MaxValue)
      val endX = doubleInRange("cropEndX", PercentPoint.MinValue, PercentPoint.MaxValue)
      val endY = doubleInRange("cropEndY", PercentPoint.MinValue, PercentPoint.MaxValue)

      (startX, startY, endX, endY) match {
        case (Some(sx), Some(sy), Some(ex), Some(ey)) =>
          imageConverter.crop(image, PercentPoint(sx.toInt, sy.toInt), PercentPoint(ex.toInt, ey.toInt))
        case _ => Success(image)
      }
    }

    def dynamicCrop(image: ImageStream): Try[ImageStream] = {
      val focalX = doubleInRange("focalX", PercentPoint.MinValue, PercentPoint.MaxValue)
      val focalY = doubleInRange("focalY", PercentPoint.MinValue, PercentPoint.MaxValue)
      val Seq(widthOpt, heightOpt) = extractDoubleOpts("width", "height")

      (focalX, focalY, widthOpt, heightOpt) match {
        case (Some(fx), Some(fy), Some(w), Some(h)) =>
          imageConverter.dynamicCrop(image, PercentPoint(fx.toInt, fy.toInt), w.toInt, h.toInt)
        case (Some(fx), Some(fy), Some(w), None) =>
          imageConverter.dynamicCropWidth(image, PercentPoint(fx.toInt, fy.toInt), w.toInt)
        case (Some(fx), Some(fy), None, Some(h)) =>
          imageConverter.dynamicCropHeight(image, PercentPoint(fx.toInt, fy.toInt), h.toInt)

        case _ => Success(image)
      }
    }

    def resize(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      val Seq(widthOpt, heightOpt) = extractDoubleOpts("width", "height")
      val focalPointIsDefined = doubleOrNone("focalX").isDefined && doubleOrNone("focalY").isDefined && (widthOpt.isDefined || heightOpt.isDefined)

      (widthOpt, heightOpt, focalPointIsDefined) match {
        case (Some(width), Some(height), false) => imageConverter.resize(image, width.toInt, height.toInt)
        case (Some(width), _, false) => imageConverter.resizeWidth(image, width.toInt)
        case (_, Some(height), false) => imageConverter.resizeHeight(image, height.toInt)
        case _ => Success(image)
      }
    }

  }
}
