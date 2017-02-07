/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import javax.servlet.http.HttpServletRequest

import com.sun.scenario.effect.Crop
import no.ndla.imageapi.model.Error
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.model.api.{ImageMetaInformation, SearchResult}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service._
import no.ndla.imageapi.service.search.SearchService
import org.scalatra.swagger.{Swagger, SwaggerSupport}

import scala.util.{Failure, Success, Try}

trait ImageController {
  this: ImageRepository with SearchService with ConverterService with ImageStorageService with ImageConverter =>
  val imageController: ImageController

  class ImageController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    // Swagger-stuff
    protected val applicationDescription = "API for accessing images from ndla.no."

    val getImages =
      (apiOperation[SearchResult]("getImages")
        summary "Show all images"
        notes "Shows all the images in the ndla.no database. You can search it too."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        queryParam[Option[String]]("query").description("Return only images with titles, alt-texts or tags matching the specified query."),
        queryParam[Option[String]]("minimum-size").description("Return only images with full size larger than submitted value in bytes."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[String]]("license").description("Return only images with provided license."),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
        ))

    val getByImageId =
      (apiOperation[ImageMetaInformation]("findByImageId")
        summary "Show image info"
        notes "Shows info of the image with submitted id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("image_id").description("Image_id of the image that needs to be fetched."))
        )


    get("/", operation(getImages)) {
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

    get("/:image_id", operation(getByImageId)) {
      val imageId = long("image_id")
      imageRepository.withId(imageId) match {
        case Some(image) => converterService.asApiImageMetaInformationWithApplicationUrl(image)
        case None => halt(status = 404, body = Error(NOT_FOUND, s"Image with id $imageId not found"))
      }
    }

    get("/full/:filename") {
      val filepath = s"full/${params("filename")}"
      imageStorage.get(filepath).flatMap(crop).flatMap(resize) match {
        case Success(img) => img
        case Failure(e) => errorHandler(e)
      }
    }

    def crop(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      intOpts("x1", "y1", "x2", "y2") match {
        case Seq(Some(x1), Some(y1), Some(x2), Some(y2)) => imageConverter.crop(image, CropOptions(x1, y1, x2, y2))
        case _ => Success(image)
      }
    }

    def resize(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
     intOpts("width", "height") match {
        case Seq(Some(width), _) => imageConverter.resize(image, width)
        case Seq(_, Some(height)) => imageConverter.resize(image, height)
        case Seq(Some(width), Some(height)) => imageConverter.resize(image, width, height)
        case _ => Success(image)
      }
    }

  }
}
