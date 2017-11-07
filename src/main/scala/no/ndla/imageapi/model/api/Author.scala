package no.ndla.imageapi.model.api

import no.ndla.imageapi.ImageApiProperties
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field



@ApiModel(description = "Information about an author")
case class Author(@(ApiModelProperty@field)(description = "The description of the author. Eg. Photographer or Supplier",
                                            allowableValues = ImageApiProperties.authorTypeString) `type`: String,
                  @(ApiModelProperty@field)(description = "The name of the of the author") name: String)
