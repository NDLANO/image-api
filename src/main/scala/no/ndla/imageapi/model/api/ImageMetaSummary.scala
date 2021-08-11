package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Summary of meta information for an image")
case class ImageMetaSummary(
    @(ApiModelProperty @field)(description = "The unique id of the image") id: String,
    @(ApiModelProperty @field)(description = "The title for this image") title: ImageTitle,
    @(ApiModelProperty @field)(description = "The copyright authors for this image") contributors: Seq[String],
    @(ApiModelProperty @field)(description = "The alt text for this image") altText: ImageAltText,
    @(ApiModelProperty @field)(description = "The full url to where a preview of the image can be downloaded") previewUrl: String,
    @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the image can be found") metaUrl: String,
    @(ApiModelProperty @field)(description = "Describes the license of the image") license: String,
    @(ApiModelProperty @field)(description = "List of supported languages in priority") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "Describes if the model has released use of the image", allowableValues = "yes,no,not-applicable") modelRelease: Option[String],
    @(ApiModelProperty @field)(description = "Describes the changes made to the image, only visible to editors") editorNotes: Option[Seq[String]]
)
