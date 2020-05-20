/*
 * Part of NDLA image-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
import no.ndla.imageapi.model.domain

@ApiModel(description = "Information about image meta dump")
case class ImageMetaDomainDump(
    @(ApiModelProperty @field)(description = "The total number of images in the database") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[domain.ImageMetaInformation])
