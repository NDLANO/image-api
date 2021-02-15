package no.ndla.imageapi.model.api

import java.util.Date
import no.ndla.imageapi.model.domain.EditorNote

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for the image")
case class ImageMetaInformationV2(
    @(ApiModelProperty @field)(description = "The unique id of the image") id: String,
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
      String],
/*    @(ApiModelProperty @field)(description = "Describes when the image was created") created: Date,
    @(ApiModelProperty @field)(description = "Describes who created the image") createdBy: String, */

    @(ApiModelProperty @field)(description = "Describes if the model has released use of the image") modelRelease: String,
    @(ApiModelProperty @field)(description = "Describes the changes made to the image") EditorNotes: Seq[EditorNote])

