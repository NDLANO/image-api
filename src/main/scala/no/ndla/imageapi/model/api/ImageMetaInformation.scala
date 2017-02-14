package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for the image")
case class ImageMetaInformation(@(ApiModelProperty@field)(description = "The unique id of the image") id: String,
                                @(ApiModelProperty@field)(description = "The url to where this information can be found") metaUrl: String,
                                @(ApiModelProperty@field)(description = "Available titles for the image") titles: Seq[ImageTitle],
                                @(ApiModelProperty@field)(description = "Available alternative texts for the image") alttexts: Seq[ImageAltText],
                                @(ApiModelProperty@field)(description = "The full url to where the image can be downloaded") imageUrl: String,
                                @(ApiModelProperty@field)(description = "The size of the image in bytes") size: Int,
                                @(ApiModelProperty@field)(description = "The mimetype of the image") contentType: String,
                                @(ApiModelProperty@field)(description = "Describes the copyright information for the image") copyright: Copyright,
                                @(ApiModelProperty@field)(description = "Searchable tags for the image") tags: Seq[ImageTag],
                                @(ApiModelProperty@field)(description = "Searchable tags for the image") captions: Seq[ImageCaption])
