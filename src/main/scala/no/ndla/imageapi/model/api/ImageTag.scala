package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "An tag for an image")
case class ImageTag(@(ApiModelProperty@field)(description = "The searchable tag.") tags: Seq[String],
                    @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in tag") language: String)
