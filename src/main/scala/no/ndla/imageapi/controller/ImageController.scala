/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ImageApiProperties._
import no.ndla.imageapi.model.Error
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.model.api.{ImageMetaInformation, SearchResult}
import no.ndla.imageapi.repository.ImageRepositoryComponent
import no.ndla.imageapi.service.{ConverterService, SearchService}
import org.scalatra.swagger.{Swagger, SwaggerSupport}

import scala.util.Try

trait ImageController {
  this: ImageRepositoryComponent with SearchService with ConverterService =>
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
        case Some(size) => if (size.forall(_.isDigit)) Option(size.toInt) else None
        case None => None
      }

      query match {
        case Some(query) => searchService.matchingQuery(
          query = query.toLowerCase().split(" ").map(_.trim),
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
  }
}
