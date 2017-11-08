package no.ndla.imageapi.model.api


import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta information for the image")
case class NewImageMetaInformation(@(ApiModelProperty@field)(description = "External id for the image") externalId: Option[String],
                                   @(ApiModelProperty@field)(description = "Available titles for the image") titles: Seq[ImageTitle],
                                   @(ApiModelProperty@field)(description = "Available alternative texts for the image") alttexts: Seq[ImageAltText],
                                   @(ApiModelProperty@field)(description = "Describes the copyright information for the image") copyright: Copyright,
                                   @(ApiModelProperty@field)(description = "Searchable tags for the image") tags: Option[Seq[ImageTag]],
                                   @(ApiModelProperty@field)(description = "Searchable tags for the image") captions: Option[Seq[ImageCaption]])
