package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Summary of meta information for an image")
case class ImageMetaSummary(@(ApiModelProperty@field)(description = "The unique id of the image") id: String,
                            @(ApiModelProperty@field)(description = "The title for this image") title: String,
                            @(ApiModelProperty@field)(description = "The alt text for this image") altText: String,
                            @(ApiModelProperty@field)(description = "The full url to where a preview of the image can be downloaded") previewUrl: String,
                            @(ApiModelProperty@field)(description = "The full url to where the complete metainformation about the image can be found") metaUrl: String,
                            @(ApiModelProperty@field)(description = "Describes the license of the image") license: String)
