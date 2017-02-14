package no.ndla.imageapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class ImageCaption(@(ApiModelProperty@field)(description = "The caption for the image") caption: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the caption") language: Option[String])
