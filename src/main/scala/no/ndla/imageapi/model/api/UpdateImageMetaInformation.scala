/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information for the image")
case class UpdateImageMetaInformation(
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language") language: String,
    @(ApiModelProperty @field)(description = "Title for the image") title: Option[String],
    @(ApiModelProperty @field)(description = "Alternative text for the image") alttext: Option[String],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the image") copyright: Option[Copyright],
    @(ApiModelProperty @field)(description = "Searchable tags for the image") tags: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "Caption for the image") caption: Option[String],
    @(ApiModelProperty @field)(description = "Describes if the model has released use of the image", allowableValues = "yes,no,not-applicable") modelReleased: Option[String]
)
// format: on
