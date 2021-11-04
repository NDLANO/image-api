/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Url and size information about the image")
case class Image(
    @(ApiModelProperty @field)(description = "The full url to where the image can be downloaded") url: String,
    @(ApiModelProperty @field)(description = "The size of the image in bytes") size: Long,
    @(ApiModelProperty @field)(description = "The mimetype of the image") contentType: String)
