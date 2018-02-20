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
import org.scalatra.util.NotNothing

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

    case class Param(param_name:String, description:String)

    private val correlationId = Param("X-Correlation-ID","User supplied correlation-id. May be omitted.")
    private val query = Param("query","Return only images with titles, alt-texts or tags matching the specified query.")
    private val minSize = Param("minimum-size","Return only images with full size larger than submitted value in bytes.")
    private val language = Param("language", "The ISO 639-1 language code describing language.")
    private val license = Param("license","Return only images with provided license.")
    private val includeCopyrighted = Param("includeCopyrighted","Return copyrighted images. May be omitted.")
    private val sort = Param("sort",
      """The sorting used on results.
             The following are supported: relevance, -relevance, title, -title, lastUpdated, -lastUpdated, id, -id.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin)
    private val pageNo = Param("page","The page number of the search hits to display.")
    private val pageSize = Param("page-size","The number of search hits to display for each page.")
    private val imageId = Param("image_id","Image_id of the image that needs to be fetched.")
    private val metadata = Param("metadata",
      """The metadata for the image file to submit. Format (as JSON):
            {
             title: String,
             alttext: String,
             tags: Array[String],
             caption: String,
             language: String
             }""".stripMargin)
    private val file = Param("file", "The image file(s) to upload")


    private def asQueryParam[T: Manifest: NotNothing](param: Param) = queryParam[T](param.param_name).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param) = headerParam[T](param.param_name).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param) = pathParam[T](param.param_name).description(param.description)
    private def asFormParam[T: Manifest: NotNothing](param: Param) = formParam[T](param.param_name).description(param.description)
    private def asFileParam(param: Param) = Parameter(name = param.param_name, `type` = ValueDataType("file"), description = Some(param.description), paramType = ParamType.Form)


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
        asHeaderParam[Option[String]](correlationId),
        asQueryParam[Option[String]](query),
        asQueryParam[Option[Int]](minSize),
        asQueryParam[Option[String]](language),
        asQueryParam[Option[String]](license),
        asQueryParam[Option[Boolean]](includeCopyrighted),
        asQueryParam[Option[String]](sort),
        asQueryParam[Option[Int]](pageNo),
        asQueryParam[Option[Int]](pageSize)
      )
        authorizations "oauth2"
        responseMessages response500)

    get("/", operation(getImages)) {
      val minimumSize = intOrNone(this.minSize.param_name)
      val query = paramOrNone(this.query.param_name)
      val language = paramOrNone(this.language.param_name)
      val license = params.get(this.license.param_name)
      val pageSize = intOrNone(this.pageSize.param_name)
      val page = intOrNone(this.pageNo.param_name)
      val sort = Sort.valueOf(paramOrDefault(this.sort.param_name, ""))
      val includeCopyrighted = booleanOrDefault(this.includeCopyrighted.param_name, false)

      search(minimumSize, query, language, license, sort, pageSize, page, includeCopyrighted)
    }

    val getImagesPost =
      (apiOperation[List[SearchResult]]("getImagesPost")
        summary "Find images"
        notes "Find images in the ndla.no database."
        parameters(
        asHeaderParam[Option[String]](correlationId),
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
      val sort = Sort.valueOf(searchParams.sort)
      val includeCopyrighted = searchParams.includeCopyrighted.getOrElse(false)

      search(minimumSize, query, language, license, sort, pageSize, page, includeCopyrighted)
    }

    val getByImageId =
      (apiOperation[ImageMetaInformationV2]("findByImageId")
        summary "Fetch information for image"
        notes "Shows info of the image with submitted id."
        parameters(
        asHeaderParam[Option[String]](correlationId),
        asPathParam[String](imageId),
        asQueryParam[String](language)
      )
        authorizations "oauth2"
        responseMessages(response404, response500))


    get("/:image_id", operation(getByImageId)) {
      val imageId = long(this.imageId.param_name)
      val language = paramOrNone(this.language.param_name)
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
        asHeaderParam[Option[String]](correlationId),
        asFormParam[String](metadata),
        asFileParam(file)
      )
        authorizations "oauth2"
        responseMessages(response400, response403, response413, response500))

    post("/", operation(newImage)) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val newImage = params.get(this.metadata.param_name)
        .map(extract[NewImageMetaInformationV2])
        .getOrElse(throw new ValidationException(errors = Seq(ValidationMessage("metadata", "The request must contain image metadata"))))

      val file = fileParams.getOrElse(this.file.param_name, throw new ValidationException(errors = Seq(ValidationMessage("file", "The request must contain an image file"))))

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
        asHeaderParam[Option[String]](correlationId),
        asPathParam[String](imageId),
        bodyParam[UpdateImageMetaInformation]("metadata").description("The metadata for the image file to submit."),
      )
        authorizations "oauth2"
        responseMessages(response400, response403, response500))

    patch("/:image_id", operation(updateImage)) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)
      val imageId = long(this.imageId.param_name)
      writeService.updateImage(imageId, extract[UpdateImageMetaInformation](request.body)) match {
        case Success(imageMeta) => imageMeta
        case Failure(e) => errorHandler(e)
      }
    }

  }

}
