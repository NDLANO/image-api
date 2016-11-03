/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Summary of meta information for an image")
case class ImageMetaSummary(@(ApiModelProperty@field)(description = "The unique id of the image") id: String,
                            @(ApiModelProperty@field)(description = "The full url to where a preview of the image can be downloaded") previewUrl: String,
                            @(ApiModelProperty@field)(description = "The full url to where the complete metainformation about the image can be found") metaUrl: String,
                            @(ApiModelProperty@field)(description = "Describes the license of the image") license: String)

@ApiModel(description = "Meta information for the image")
case class ImageMetaInformation(@(ApiModelProperty@field)(description = "The unique id of the image") id: String,
                                @(ApiModelProperty@field)(description = "The url to where this information can be found") metaUrl: String,
                                @(ApiModelProperty@field)(description = "Available titles for the image") titles: Seq[ImageTitle],
                                @(ApiModelProperty@field)(description = "Available alternative texts for the image") alttexts: Seq[ImageAltText],
                                @(ApiModelProperty@field)(description = "The full url to where the image can be downloaded") url: String,
                                @(ApiModelProperty@field)(description = "The size of the image in bytes") size: Int,
                                @(ApiModelProperty@field)(description = "The mimetype of the image") contentType: String,
                                @(ApiModelProperty@field)(description = "Describes the copyright information for the image") copyright: Copyright,
                                @(ApiModelProperty@field)(description = "Searchable tags for the image") tags: Seq[ImageTag],
                                @(ApiModelProperty@field)(description = "Searchable tags for the image") captions: Seq[ImageCaption])

case class ImageTitle(@(ApiModelProperty@field)(description = "The freetext title of the image") title: String,
                      @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: Option[String])

case class ImageAltText(@(ApiModelProperty@field)(description = "The alternative text for the image") alttext: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the alternative text") language: Option[String])

case class ImageCaption(@(ApiModelProperty@field)(description = "The caption for the image") caption: String,
                        @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the caption") language: Option[String])

case class ImageTag(@(ApiModelProperty@field)(description = "The searchable tag.") tags: Seq[String],
                    @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in tag") language: Option[String])

@ApiModel(description = "Url and size information about the image")
case class Image(@(ApiModelProperty@field)(description = "The full url to where the image can be downloaded") url: String,
                 @(ApiModelProperty@field)(description = "The size of the image in bytes") size: Int,
                 @(ApiModelProperty@field)(description = "The mimetype of the image") contentType: String)

@ApiModel(description = "Description of copyright information")
case class Copyright(@(ApiModelProperty@field)(description = "Describes the license of the image") license: License,
                     @(ApiModelProperty@field)(description = "Reference to where the image is procured") origin: String,
                     @(ApiModelProperty@field)(description = "List of authors") authors: Seq[Author])

@ApiModel(description = "Description of license information")
case class License(@(ApiModelProperty@field)(description = "The name of the license") license: String,
                   @(ApiModelProperty@field)(description = "Description of the license") description: String,
                   @(ApiModelProperty@field)(description = "Url to where the license can be found") url: Option[String])

@ApiModel(description = "Information about an author")
case class Author(@(ApiModelProperty@field)(description = "The description of the author. Eg. Photographer or Supplier") `type`: String,
                  @(ApiModelProperty@field)(description = "The name of the of the author") name: String)

@ApiModel(description = "Information about search-results")
case class SearchResult(@(ApiModelProperty@field)(description = "The total number of images matching this query") totalCount: Long,
                        @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                        @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                        @(ApiModelProperty@field)(description = "The search results") results: Seq[ImageMetaSummary])
