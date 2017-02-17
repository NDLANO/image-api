/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import java.text.SimpleDateFormat
import java.util.Date

import io.swagger.annotations.{ApiModel, ApiModelProperty}


@ApiModel(description = "Summary of meta information for an image")
case class ImageMetaSummary(@ApiModelProperty(value = "The unique id of the image") id: String,
                            @ApiModelProperty(value = "The full url to where a preview of the image can be downloaded") previewUrl: String,
                            @ApiModelProperty(value = "The full url to where the complete metainformation about the image can be found") metaUrl: String,
                            @ApiModelProperty(value = "Describes the license of the image") license: String)

@ApiModel(description = "Meta information for the image")
case class ImageMetaInformation(@ApiModelProperty(value = "The unique id of the image") id: String,
                                @ApiModelProperty(value = "The url to where this information can be found") metaUrl: String,
                                @ApiModelProperty(value = "Available titles for the image") titles: Seq[ImageTitle],
                                @ApiModelProperty(value = "Available alternative texts for the image") alttexts: Seq[ImageAltText],
                                @ApiModelProperty(value = "The full url to where the image can be downloaded") imageUrl: String,
                                @ApiModelProperty(value = "The size of the image in bytes") size: Int,
                                @ApiModelProperty(value = "The mimetype of the image") contentType: String,
                                @ApiModelProperty(value = "Describes the copyright information for the image") copyright: Copyright,
                                @ApiModelProperty(value = "Searchable tags for the image") tags: Seq[ImageTag],
                                @ApiModelProperty(value = "Searchable tags for the image") captions: Seq[ImageCaption])

case class ImageTitle(@ApiModelProperty title: String,
                      @ApiModelProperty(value = "ISO 639-1 code that represents the language used in title") language: Option[String])

case class ImageAltText(@ApiModelProperty(value = "The alternative text for the image") alttext: String,
                        @ApiModelProperty(value = "ISO 639-1 code that represents the language used in the alternative text") language: Option[String])

case class ImageCaption(@ApiModelProperty(value = "The caption for the image") caption: String,
                        @ApiModelProperty(value = "ISO 639-1 code that represents the language used in the caption") language: Option[String])

case class ImageTag(@ApiModelProperty(value = "The searchable tag.") tags: Seq[String],
                    @ApiModelProperty(value = "ISO 639-1 code that represents the language used in tag") language: Option[String])

@ApiModel(description = "Url and size information about the image")
case class Image(@ApiModelProperty(value = "The full url to where the image can be downloaded") url: String,
                 @ApiModelProperty(value = "The size of the image in bytes") size: Int,
                 @ApiModelProperty(value = "The mimetype of the image") contentType: String)

@ApiModel(description = "Description of copyright information")
case class Copyright(@ApiModelProperty(value = "Describes the license of the image") license: License,
                     @ApiModelProperty(value = "Reference to where the image is procured") origin: String,
                     @ApiModelProperty(value = "List of authors") authors: Seq[Author])

@ApiModel(description = "Description of license information")
case class License(@ApiModelProperty(value = "The name of the license") license: String,
                   @ApiModelProperty(value = "Description of the license") description: String,
                   @ApiModelProperty(value = "Url to where the license can be found") url: Option[String])

@ApiModel(description = "Information about an author")
case class Author(@ApiModelProperty(value = "The description of the author. Eg. Photographer or Supplier") `type`: String,
                  @ApiModelProperty(value = "The name of the of the author") name: String)

@ApiModel(description = "Information about search-results")
case class SearchResult(@ApiModelProperty(value = "The total number of images matching this query", required = true) totalCount: Long,
                        @ApiModelProperty(value = "For which page results are shown from", required = true) page: Int,
                        @ApiModelProperty(value = "The number of results per page", required = true) pageSize: Int,
                        @ApiModelProperty(value = "The search results", required = true) results: Seq[ImageMetaSummary])

@ApiModel(description = "Information about an error")
case class Error(@ApiModelProperty(value = "A code describing the error that occured") code: String,
                 @ApiModelProperty(value = "A description of the error") description: String,
                 @ApiModelProperty(value = "Timestamp on format yyyy-MM-dd HH:mm:ss.SSS") occuredAt:String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))