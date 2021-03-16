package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for the image")
case class ImageMetaInformationV3(
    @(ApiModelProperty @field)(description = "The unique id of the image") id: Long,
    @(ApiModelProperty @field)(description = "The url to where this information can be found") metaUrl: String,
    @(ApiModelProperty @field)(description = "The title for the image") title: ImageTitle,
    @(ApiModelProperty @field)(description = "Alternative text for the image") alttext: ImageAltText,
    @(ApiModelProperty @field)(description = "The full url to where the image can be downloaded") imageUrl: String,
    @(ApiModelProperty @field)(description = "The size of the image in bytes") size: Long,
    @(ApiModelProperty @field)(description = "The mimetype of the image") contentType: String,
    @(ApiModelProperty @field)(description = "Describes the copyright information for the image") copyright: Copyright,
    @(ApiModelProperty @field)(description = "Searchable tags for the image") tags: ImageTag,
    @(ApiModelProperty @field)(description = "Searchable caption for the image") caption: ImageCaption,
    @(ApiModelProperty @field)(description = "Supported languages for the image title, alt-text, tags and caption.") supportedLanguages: Seq[
      String])
