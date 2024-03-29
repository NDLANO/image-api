/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import no.ndla.imageapi.model.domain.{EditorNote, ModelReleasedStatus}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information for the image")
case class NewImageMetaInformationV2(
    @(ApiModelProperty @field)(description = "Title for the image") title: String,
    @(ApiModelProperty @field)(description = "Alternative text for the image") alttext: String,
    @(ApiModelProperty @field)(description = "Describes the copyright information for the image") copyright: Copyright,
    @(ApiModelProperty @field)(description = "Searchable tags for the image") tags: Seq[String],
    @(ApiModelProperty @field)(description = "Caption for the image") caption: String,
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in the caption") language: String,
    @(ApiModelProperty @field)(description = "Describes if the model has released use of the image, allowed values are 'not-set', 'yes', 'no', and 'not-applicable', defaults to 'no'", allowableValues = "yes,no,not-applicable") modelReleased: Option[String]
)
