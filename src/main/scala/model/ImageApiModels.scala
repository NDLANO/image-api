package model

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for the image")
case class ImageMetaInformation(
  @(ApiModelProperty @field)(description = "The unique id of the image.") id:String,
  @(ApiModelProperty @field)(description = "The freetext title of the image.") title:String,
  @(ApiModelProperty @field)(description = "The possible size variants of the image.") images:ImageVariants,
  @(ApiModelProperty @field)(description = "Describes the copyright information for the image.") copyright:Copyright,
  @(ApiModelProperty @field)(description = "Searchable tags for the image.") tags:Iterable[String]
)

@ApiModel(description = "The possible variants of the image")
case class ImageVariants(
  @(ApiModelProperty @field)(description = "The thumbsize image") small: Option[Image],
  @(ApiModelProperty @field)(description = "The fullsize image") full: Option[Image]
)

@ApiModel(description = "Url and size information about the image")
case class Image(
  @(ApiModelProperty @field)(description = "The full url to where the image can be downloaded.") url:String,
  @(ApiModelProperty @field)(description = "The size of the image in KB") size:String,
  @(ApiModelProperty @field)(description = "The mimetype of the image") contentType:String
)

@ApiModel(description = "Description of copyright information")
case class Copyright(
  @(ApiModelProperty @field)(description = "Describes the license of the image.") license:String,
  @(ApiModelProperty @field)(description = "Reference to where the image is procured.") origin:String,
  @(ApiModelProperty @field)(description = "List of authors") authors:Iterable[Author]
)

@ApiModel(description = "Information about an author")
case class Author(
  @(ApiModelProperty @field)(description = "The description of the author. Eg. Photographer or Supplier") `type`:String,
  @(ApiModelProperty @field)(description = "The name of the of the author.") name:String
)
