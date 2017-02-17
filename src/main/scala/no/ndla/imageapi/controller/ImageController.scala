/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import javax.servlet.http.HttpServletRequest

import io.swagger.annotations._
import no.ndla.imageapi.model.{api, Error}
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.ConverterService
import no.ndla.imageapi.service.search.SearchService

import scala.util.Try

trait ImageController {
  this: ImageRepository with SearchService with ConverterService =>
  val imageController: ImageController

  @Api(value = "/image-api/v1/images", authorizations = Array(new Authorization(value = "imageapi_auth")))
  class ImageController extends NdlaController {
    get("/") (getImages)
    get("/:image_id")(getImage)

    @ApiOperation(nickname = "/", httpMethod = "get", value = "Show all images", notes = "Shows all the images in the ndla.no database. You can search it too.", tags = Array("ImageApi-V1"))
    @ApiImplicitParams(value = Array(
      new ApiImplicitParam(name = "X-Correlation-ID", value = "User supplied correlation-id. May be omitted.", dataType = "string", paramType = "header"),
      new ApiImplicitParam(name = "query", value = "Return only images with titles, alt-texts or tags matching the specified query.", dataType = "string", paramType = "query"),
      new ApiImplicitParam(name = "minimum-size", value = "Return only images with full size larger than submitted value in bytes.", dataType = "string", paramType = "query"),
      new ApiImplicitParam(name = "language", value = "The ISO 639-1 language code describing language used in query-params.", dataType = "string", paramType = "query"),
      new ApiImplicitParam(name = "license", value = "Return only images with provided license.", dataType = "string", paramType = "query"),
      new ApiImplicitParam(name = "page", value = "The page number of the search hits to display.", dataType = "integer", paramType = "query"),
      new ApiImplicitParam(name = "page-size", value = "The number of search hits to display for each page.", dataType = "integer", paramType = "query")))
    @ApiResponses(Array(new ApiResponse(code = 500, message = "Internal server error", response = classOf[api.Error])))
    private def getImages(implicit request: HttpServletRequest) = {
      val minimumSize = params.get("minimum-size")
      val query = params.get("query")
      val language = params.get("language")
      val license = params.get("license")
      val pageSize = params.get("page-size").flatMap(ps => Try(ps.toInt).toOption)
      val page = params.get("page").flatMap(idx => Try(idx.toInt).toOption)

      val size = minimumSize match {
        case Some(toCheck) => if (toCheck.forall(_.isDigit)) Option(toCheck.toInt) else None
        case None => None
      }

      query match {
        case Some(searchString) => searchService.matchingQuery(
          query = searchString.trim,
          minimumSize = size,
          language = language,
          license = license,
          page,
          pageSize)

        case None => searchService.all(minimumSize = size, license = license, page, pageSize)
      }
    }

    @ApiOperation(nickname = "/{image_id}", httpMethod = "get", value = "Show image info", notes = "Show image info", tags = Array("ImageApi-V1"))
    @ApiImplicitParams(value = Array(
      new ApiImplicitParam(name = "X-Correlation-ID", value = "User supplied correlation-id. May be omitted.", dataType = "string", paramType = "header"),
      new ApiImplicitParam(name = "image_id", value = "Image_id of the image that needs to be fetched.", dataType = "string", paramType = "path")))
    @ApiResponses(Array(
      new ApiResponse(code = 404, message = "Not Found", response = classOf[api.Error]),
      new ApiResponse(code = 500, message = "Internal server error", response = classOf[api.Error])))
    private def getImage(implicit request: HttpServletRequest)  = {
      val imageId = long("image_id")
      imageRepository.withId(imageId) match {
        case Some(image) => converterService.asApiImageMetaInformationWithApplicationUrl(image)
        case None => halt(status = 404, body = api.Error(NOT_FOUND, s"Image with id $imageId not found"))
      }
    }
  }
}
