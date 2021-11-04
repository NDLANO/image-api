/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import java.util.Date
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Note about a change that happened to the image")
case class EditorNote(
  @(ApiModelProperty @field)(description = "Timestamp of the change") timestamp: Date,
  @(ApiModelProperty @field)(description = "Who triggered the change") updatedBy: String,
  @(ApiModelProperty @field)(description = "Editorial note") note: String
)
