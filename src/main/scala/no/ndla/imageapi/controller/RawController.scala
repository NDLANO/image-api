package no.ndla.imageapi.controller

import javax.servlet.http.HttpServletRequest

import io.swagger.annotations._
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.model.{ImageNotFoundException, api}
import no.ndla.imageapi.service.{ImageConverter, ImageStorageService}

import scala.util.{Failure, Success, Try}

trait RawController {
  this: ImageStorageService with ImageConverter =>
  val rawController: RawController

  @Api(value = "/image-api/v1/raw", authorizations = Array(new Authorization(value = "imageapi_auth")))
  class RawController extends NdlaController {
    get("/:name")(getRaw)

    @ApiOperation(nickname = "/{name}", httpMethod = "get", value = "Fetches a raw image", notes = "Fetches an image with options to resize and crop", tags = Array("ImageApi-V1"))
    @ApiImplicitParams(value = Array(
      new ApiImplicitParam(name = "X-Correlation-ID", value = "User supplied correlation-id. May be omitted.", dataType = "string", paramType = "header"),
      new ApiImplicitParam(name = "name", value = "The name of the image", dataType = "string", paramType = "path"),
      new ApiImplicitParam(name = "width", value = "The target width to resize the image. Image proportions are kept intact.", dataType = "Integer", paramType = "query"),
      new ApiImplicitParam(name = "height", value = "The target height to resize the image. Image proportions are kept intact", dataType = "Integer", paramType = "query"),
      new ApiImplicitParam(name = "cropStart", value = "The first image coordinate (X,Y) specifying the crop start position. The coordinate is a comma separated value of the form column,row (e.g 100,10). If cropStart is specified cropEnd must also be specified", dataType = "string", paramType = "query"),
      new ApiImplicitParam(name = "cropEnd", value = "The second image coordinate (X,Y) specifying the crop end position, forming a square cutout. The coordinate is a comma separated value of the form column,row (e.g 200,100). If cropEnd is specified cropStart must also be specified", dataType = "integer", paramType = "query")))
    @ApiResponses(Array(
      new ApiResponse(code = 404, message = "Not found", response = classOf[api.Error]),
      new ApiResponse(code = 500, message = "Internal server error", response = classOf[api.Error])))
    private def getRaw(implicit request: HttpServletRequest) = {
      val imageName = params("name")
      imageStorage.get(imageName).flatMap(crop).flatMap(resize) match {
        case Success(img) => img
        case Failure(_) => errorHandler(new ImageNotFoundException(s"image $imageName does not exist"))
      }
    }

    def crop(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      (paramAsListOfInt("cropStart"), paramAsListOfInt("cropEnd")) match {
        case (List(startX, startY), List(endX, endY)) => imageConverter.crop(image, Point(startX, startY), Point(endX, endY))
        case _ => Success(image)
      }
    }

    def resize(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      extractIntOpts("width", "height") match {
        case Seq(Some(width), _) => imageConverter.resizeWidth(image, width)
        case Seq(_, Some(height)) => imageConverter.resizeHeight(image, height)
        case Seq(Some(width), Some(height)) => imageConverter.resize(image, width, height)
        case _ => Success(image)
      }
    }

  }
}
