/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Alt-text of an image")
case class ImageAltText(@(ApiModelProperty @field)(description = "The alternative text for the image") alttext: String,
                        @(ApiModelProperty @field)(description =
                          "ISO 639-1 code that represents the language used in the alternative text") language: String)
