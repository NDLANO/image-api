package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Url and size information about the image")
case class NewImageFile(@(ApiModelProperty @field)(description = "The name of the file") fileName: String,
                        @(ApiModelProperty @field)(description =
                          "ISO 639-1 code that represents the language used in the audio") language: Option[String])
