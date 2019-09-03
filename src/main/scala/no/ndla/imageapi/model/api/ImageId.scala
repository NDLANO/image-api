package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Object with id of an image")
case class ImageId(@(ApiModelProperty @field)(description = "The id of an image") id: Long)
