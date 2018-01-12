package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about search-results")
case class SearchResult(@(ApiModelProperty@field)(description = "The total number of images matching this query") totalCount: Long,
                        @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                        @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                        @(ApiModelProperty@field)(description = "The chosen search language") language: String,
                        @(ApiModelProperty@field)(description = "The search results") results: Seq[ImageMetaSummary])
