/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.integration.DraftApiClient
import no.ndla.imageapi.model.api.{ImageMetaInformationV2, ImageMetaInformationV3, ImageMetaSummary}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageSearchService, SearchConverterService}
import no.ndla.imageapi.service.{ConverterService, ReadService, WriteService}
import org.scalatra.{RenderPipeline, Route, RouteTransformer}
import org.scalatra.servlet.FileUploadSupport
import org.scalatra.swagger._

trait ImageControllerV2 {
  this: ImageRepository
    with ImageSearchService
    with ConverterService
    with ReadService
    with WriteService
    with DraftApiClient
    with SearchConverterService
    with ImageControllerV3
    with Role
    with User =>
  val imageControllerV2: ImageControllerV2

  class ImageControllerV2(implicit override val swagger: Swagger)
      extends ImageControllerV3
      with SwaggerSupport
      with FileUploadSupport {

    override val deprecated = true

    private val v3tov2Conversion: RenderPipeline = {
      case meta: ImageMetaInformationV3 =>
        ImageMetaInformationV2(
          meta.id.toString,
          meta.metaUrl,
          meta.title,
          meta.alttext,
          meta.imageUrl,
          meta.size,
          meta.contentType,
          meta.copyright,
          meta.tags,
          meta.caption,
          meta.supportedLanguages
        )
    }

    override def renderPipeline = v3tov2Conversion orElse super.renderPipeline
  }

}
