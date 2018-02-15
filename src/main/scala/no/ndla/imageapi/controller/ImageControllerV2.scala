/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ImageApiProperties.{MaxImageFileSizeBytes, RoleWithWriteAccess}
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.integration.DraftApiClient
import no.ndla.imageapi.model.api.{Error, ImageMetaInformationV2, License, NewImageMetaInformationV2, SearchParams, SearchResult, UpdateImageMetaInformation, ValidationError}
import no.ndla.imageapi.model.domain.Sort
import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.SearchService
import no.ndla.imageapi.service.{ConverterService, WriteService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger._

import scala.util.{Failure, Success}

trait ImageControllerV2 {
  this: ImageRepository with SearchService with ConverterService with WriteService with DraftApiClient with Role with User =>
  val imageControllerV2: ImageControllerV2

  class ImageControllerV2(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport with FileUploadSupport {
    // Swagger-stuff
    protected val applicationDescription = "Services for accessing images"
    protected implicit override val jsonFormats: Formats = DefaultFormats

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()
    registerModel[NewImageMetaInformationV2]()

    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response413 = ResponseMessage(413, "File too big", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxImageFileSizeBytes)))

    private def search(minimumSize: Option[Int], query: Option[String], language: Option[String], license: Option[String], sort: Option[Sort.Value], pageSize: Option[Int], page: Option[Int], includeCopyrighted: Boolean) = {
      query match {
        case Some(searchString) => searchService.matchingQuery(
          query = searchString.trim,
          minimumSize = minimumSize,
          language = language,
          license = license,
          sort.getOrElse(Sort.ByRelevanceDesc),
          page,
          pageSize,
          includeCopyrighted)

        case None => searchService.all(minimumSize = minimumSize, license = license, language = language, sort = sort.getOrElse(Sort.ByTitleAsc), page, pageSize, includeCopyrighted)
      }
    }

    val getImages =
      (apiOperation[SearchResult]("getImages")
        summary "Find images"
        notes "Find images in the ndla.no database."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        queryParam[Option[String]]("query").description("Return only images with titles, alt-texts or tags matching the specified query."),
        queryParam[Option[String]]("minimum-size").description("Return only images with full size larger than submitted value in bytes."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params."),
        queryParam[Option[String]]("license").description("Return only images with provided license."),
        queryParam[Option[String]]("includeCopyrighted").description("Return copyrighted images. May be omitted."),
        queryParam[Option[String]]("sort").description(
          """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin),
        queryParam[Option[Int]]("page").description("The page number of the search hits to display."),
        queryParam[Option[Int]]("page-size").description("The number of search hits to display for each page.")
      )
        authorizations "oauth2"
        responseMessages response500)

    get("/", operation(getImages)) {
      val minimumSize = intOrNone("minimum-size")
      val query = paramOrNone("query")
      val language = paramOrNone("language")
      val license = params.get("license")
      val pageSize = intOrNone("page-size")
      val page = intOrNone("page")
      val sort = Sort.valueOf(paramOrDefault("sort", ""))
      val includeCopyrighted = booleanOrDefault("includeCopyrighted", false)

      search(minimumSize, query, language, license, sort, pageSize, page, includeCopyrighted)
    }

    val getImagesPost =
      (apiOperation[List[SearchResult]]("getImagesPost")
        summary "Find images"
        notes "Find images in the ndla.no database."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id"),
        queryParam[Option[String]]("sort").description(
          """The sorting used on results.
             Default is by -relevance (desc) when querying.
             When browsing, the default is title (asc).
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id""".stripMargin),
        bodyParam[SearchParams]
      )
        authorizations "oauth2"
        responseMessages(response400, response500))

    post("/search/", operation(getImagesPost)) {
      val searchParams = extract[SearchParams](request.body)
      val minimumSize = searchParams.minimumSize
      val query = searchParams.query
      val language = searchParams.language
      val license = searchParams.license
      val pageSize = searchParams.pageSize
      val page = searchParams.page
      val sort = Sort.valueOf(paramOrDefault("sort", ""))
      val includeCopyrighted = searchParams.includeCopyrighted.getOrElse(false)

      search(minimumSize, query, language, license, sort, pageSize, page, includeCopyrighted)
    }

    val getByImageId =
      (apiOperation[ImageMetaInformationV2]("findByImageId")
        summary "Fetch information for image"
        notes "Shows info of the image with submitted id."
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        pathParam[String]("image_id").description("Image_id of the image that needs to be fetched."),
        queryParam[Option[String]]("language").description("The ISO 639-1 language code describing language used in query-params.")
      )
        authorizations "oauth2"
        responseMessages(response404, response500))


    get("/:image_id", operation(getByImageId)) {
      val imageId = long("image_id")
      val language = paramOrNone("language")
      imageRepository.withId(imageId).flatMap(image => converterService.asApiImageMetaInformationWithApplicationUrlV2(image, language)) match {
        case Some(image) => image
        case None => halt(status = 404, body = Error(Error.NOT_FOUND, s"Image with id $imageId and language $language not found"))
      }
    }

    val newImage =
      (apiOperation[ImageMetaInformationV2]("newImage")
        summary "Upload a new image with meta information"
        notes "Upload a new image file with meta data"
        consumes "multipart/form-data"
        parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        formParam[String]("metadata").description("The metadata for the image file to submit. See NewImageMetaInformationV2."),
        Parameter(name = "file", `type` = ValueDataType("file"), description = Some("The image file(s) to upload"), paramType = ParamType.Form)
      )
        authorizations "oauth2"
        responseMessages(response400, response403, response413, response500))

    post("/", operation(newImage)) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val newImage = params.get("metadata")
        .map(extract[NewImageMetaInformationV2])
        .getOrElse(throw new ValidationException(errors = Seq(ValidationMessage("metadata", "The request must contain image metadata"))))

      val file = fileParams.getOrElse("file", throw new ValidationException(errors = Seq(ValidationMessage("file", "The request must contain an image file"))))

      writeService.storeNewImage(newImage, file)
        .map(img => converterService.asApiImageMetaInformationWithApplicationUrlV2(img, Some(newImage.language))) match {
        case Success(imageMeta) => imageMeta
        case Failure(e) => errorHandler(e)
      }
    }

    val updateImage =
      (apiOperation[ImageMetaInformationV2]("newImage")
        summary "Update an existing image with meta information"
        notes "Updates an existing image with meta data."
        consumes "form-data"
        parameters(
        pathParam[String]("image_id").description("Image_id of the image that needs to be fetched."),
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        bodyParam[UpdateImageMetaInformation]("metadata").description("The metadata for the image file to submit. See UpdateImageMetaInformation."),
      )
        authorizations "oauth2"
        responseMessages(response400, response403, response500))

    patch("/:image_id", operation(updateImage)) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)
      val imageId = long("image_id")
      writeService.updateImage(imageId, extract[UpdateImageMetaInformation](request.body)) match {
        case Success(imageMeta) => imageMeta
        case Failure(e) => errorHandler(e)
      }
    }

  }

}
