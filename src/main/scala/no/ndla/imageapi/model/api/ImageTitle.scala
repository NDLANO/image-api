/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class ImageTitle(@(ApiModelProperty@field)(description = "The freetext title of the image") title: String,
                      @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: Option[String])
