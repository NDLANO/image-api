/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ImageApiProperties.{
  MaxImageFileSizeBytes,
  RoleWithWriteAccess,
  ElasticSearchScrollKeepAlive,
  ElasticSearchIndexMaxResultWindow
}
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.integration.DraftApiClient
import no.ndla.imageapi.model.api.{
  Error,
  ImageMetaInformationV2,
  NewImageMetaInformationV2,
  SearchParams,
  SearchResult,
  UpdateImageMetaInformation,
  ValidationError
}
import no.ndla.imageapi.model.domain.Sort
import no.ndla.imageapi.model.{Language, ValidationException, ValidationMessage}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{SearchConverterService, SearchService}
import no.ndla.imageapi.service.{ConverterService, WriteService}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.Ok
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger._
import org.scalatra.util.NotNothing

import scala.util.{Failure, Success}

trait ImageControllerV2 {
  this: ImageRepository
    with SearchService
    with ConverterService
    with WriteService
    with DraftApiClient
    with SearchConverterService
    with Role
    with User =>
  val imageControllerV2: ImageControllerV2

  class ImageControllerV2(implicit val swagger: Swagger)
      extends NdlaController
      with SwaggerSupport
      with FileUploadSupport {
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

    case class Param[T](paramName: String, description: String)

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only images with titles, alt-texts or tags matching the specified query.")
    private val minSize =
      Param[Option[Int]]("minimum-size", "Return only images with full size larger than submitted value in bytes.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license = Param[Option[String]]("license", "Return only images with provided license.")
    private val includeCopyrighted =
      Param[Option[Boolean]]("includeCopyrighted", "Return copyrighted images. May be omitted.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]]("page-size", "The number of search hits to display for each page.")
    private val imageId = Param[String]("image_id", "Image_id of the image that needs to be fetched.")
    private val metadata = Param[NewImageMetaInformationV2](
      "metadata",
      """The metadata for the image file to submit.""".stripMargin
    )
    private val file = Param("file", "The image file(s) to upload")

    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A search context retrieved from the response header of a previous search.
         |If search-context is specified, all other query parameters, except '${this.language.paramName}' is ignored.
         |For the rest of the parameters the original search of the search-context is used.
         |The search context may change between scrolls. Always use the most recent one (The context if unused dies after $ElasticSearchScrollKeepAlive).
         |Used to enable scrolling past $ElasticSearchIndexMaxResultWindow results.
      """.stripMargin
    )

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)
    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)
    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)
    private def asObjectFormParam[T: Manifest: NotNothing](param: Param[T]) = {
      val className = manifest[T].runtimeClass.getSimpleName
      val modelOpt = models.get(className)

      modelOpt match {
        case Some(value) =>
          formParam(param.paramName, value).description(param.description)
        case None =>
          logger.error(s"${param.paramName} could not be resolved as object formParam, doing regular formParam.")
          formParam[T](param.paramName).description(param.description)
      }
    }
    private def asFileParam(param: Param[_]) =
      Parameter(name = param.paramName,
                `type` = ValueDataType("file"),
                description = Some(param.description),
                paramType = ParamType.Form)

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxImageFileSizeBytes)))

    /**
      * Does a scroll with [[SearchService]]
      * If no scrollId is specified execute the function @orFunction in the second parameter list.
      *
      * @param orFunction Function to execute if no scrollId in parameters (Usually searching)
      * @return A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollOr(orFunction: => Any): Any = {
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      paramOrNone(this.scrollId.paramName) match {
        case Some(scroll) =>
          searchService.scroll(scroll, language) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiSearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case None => orFunction
      }
    }

    private def search(minimumSize: Option[Int],
                       query: Option[String],
                       language: Option[String],
                       license: Option[String],
                       sort: Option[Sort.Value],
                       pageSize: Option[Int],
                       page: Option[Int],
                       includeCopyrighted: Boolean) = {
      val result = query match {
        case Some(searchString) =>
          searchService.matchingQuery(
            query = searchString.trim,
            minimumSize = minimumSize,
            language = language,
            license = license,
            sort.getOrElse(Sort.ByRelevanceDesc),
            page,
            pageSize,
            includeCopyrighted
          )
        case None =>
          searchService.all(
            minimumSize = minimumSize,
            license = license,
            language = language,
            sort = sort.getOrElse(Sort.ByTitleAsc),
            page,
            pageSize,
            includeCopyrighted
          )
      }
      result match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiSearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/",
      operation(
        apiOperation[SearchResult]("getImages")
          summary "Find images"
          description "Find images in the ndla.no database."
          parameters (
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(minSize),
            asQueryParam(language),
            asQueryParam(license),
            asQueryParam(includeCopyrighted),
            asQueryParam(sort),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(scrollId)
        )
          authorizations "oauth2"
          responseMessages response500)
    ) {
      scrollOr {
        val minimumSize = intOrNone(this.minSize.paramName)
        val query = paramOrNone(this.query.paramName)
        val language = paramOrNone(this.language.paramName)
        val license = params.get(this.license.paramName)
        val pageSize = intOrNone(this.pageSize.paramName)
        val page = intOrNone(this.pageNo.paramName)
        val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
        val includeCopyrighted = booleanOrDefault(this.includeCopyrighted.paramName, default = false)

        search(minimumSize, query, language, license, sort, pageSize, page, includeCopyrighted)
      }
    }

    post(
      "/search/",
      operation(
        apiOperation[List[SearchResult]]("getImagesPost")
          summary "Find images"
          description "Find images in the ndla.no database."
          parameters (
            asHeaderParam(correlationId),
            bodyParam[SearchParams],
            asQueryParam(scrollId)
        )
          authorizations "oauth2"
          responseMessages (response400, response500))
    ) {
      scrollOr {
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
    }

    get(
      "/:image_id",
      operation(
        apiOperation[ImageMetaInformationV2]("findByImageId")
          summary "Fetch information for image"
          description "Shows info of the image with submitted id."
          parameters (
            asHeaderParam(correlationId),
            asPathParam(imageId),
            asQueryParam(language)
        )
          authorizations "oauth2"
          responseMessages (response404, response500))
    ) {
      val imageId = long(this.imageId.paramName)
      val language = paramOrNone(this.language.paramName)
      imageRepository
        .withId(imageId)
        .flatMap(image => converterService.asApiImageMetaInformationWithApplicationUrlV2(image, language)) match {
        case Some(image) => image
        case None =>
          halt(status = 404, body = Error(Error.NOT_FOUND, s"Image with id $imageId and language $language not found"))
      }
    }

    post(
      "/",
      operation(
        apiOperation[ImageMetaInformationV2]("newImage")
          summary "Upload a new image with meta information"
          description "Upload a new image file with meta data"
          consumes "multipart/form-data"
          parameters (
            asHeaderParam(correlationId),
            asObjectFormParam(metadata),
            formParam(metadata.paramName, models("NewImageMetaInformationV2")),
            asFileParam(file)
        )
          authorizations "oauth2"
          responseMessages (response400, response403, response413, response500))
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val imageMetaFromFile = fileParams
        .get(this.metadata.paramName)
        .map(f => scala.io.Source.fromInputStream(f.getInputStream).mkString)
      val imageMetaFromParam = params.get(this.metadata.paramName)

      val imageMeta = imageMetaFromParam
        .orElse(imageMetaFromFile)
        .map(extract[NewImageMetaInformationV2])
        .getOrElse(throw new ValidationException(
          errors = Seq(ValidationMessage("metadata", "The request must contain image metadata"))))

      val file =
        fileParams.getOrElse(this.file.paramName,
                             throw new ValidationException(
                               errors = Seq(ValidationMessage("file", "The request must contain an image file"))))

      writeService
        .storeNewImage(imageMeta, file)
        .map(img => converterService.asApiImageMetaInformationWithApplicationUrlV2(img, Some(imageMeta.language))) match {
        case Success(meta) => meta
        case Failure(e)    => errorHandler(e)
      }
    }

    patch(
      "/:image_id",
      operation(
        apiOperation[ImageMetaInformationV2]("newImage")
          summary "Update an existing image with meta information"
          description "Updates an existing image with meta data."
          consumes "form-data"
          parameters (
            asHeaderParam(correlationId),
            asPathParam(imageId),
            bodyParam[UpdateImageMetaInformation]("metadata").description("The metadata for the image file to submit."),
        )
          authorizations "oauth2"
          responseMessages (response400, response403, response500))
    ) {
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)
      val imageId = long(this.imageId.paramName)
      writeService.updateImage(imageId, extract[UpdateImageMetaInformation](request.body)) match {
        case Success(imageMeta) => imageMeta
        case Failure(e)         => errorHandler(e)
      }
    }

  }

}
