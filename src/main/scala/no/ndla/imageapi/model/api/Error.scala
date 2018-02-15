/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import java.text.SimpleDateFormat
import java.util.Date

import no.ndla.imageapi.ImageApiProperties
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about errors")
case class Error(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String,
                 @(ApiModelProperty@field)(description = "Description of the error") description: String,
                 @(ApiModelProperty@field)(description = "When the error occurred") occurredAt: String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT FOUND"
  val INDEX_MISSING = "INDEX MISSING"
  val VALIDATION = "VALIDATION"
  val FILE_TOO_BIG = "FILE TOO BIG"
  val ACCESS_DENIED = "ACCESS DENIED"
  val GATEWAY_TIMEOUT =  "GATEWAY TIMEOUT"
  val WINDOW_TOO_LARGE = "RESULT WINDOW TOO LARGE"
  val IMPORT_FAILED = "IMPORT FAILED"
  val DATABASE_UNAVAILABLE = "DATABASE_UNAVAILABLE"

  val GenericError = Error(GENERIC, s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
  val IndexMissingError = Error(INDEX_MISSING, s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
  val FileTooBigError = Error(FILE_TOO_BIG, s"The file is too big. Max file size is ${ImageApiProperties.MaxImageFileSizeBytes / 1024 / 1024} MiB")
  val ImageNotFoundError = Error(NOT_FOUND, s"Ooops. That image does not exists")
  val WindowTooLargeError = Error(WINDOW_TOO_LARGE, s"The result window is too large. Fetching pages above ${ImageApiProperties.ElasticSearchIndexMaxResultWindow} results are unsupported.")
  val DatabaseUnavailableError = Error(DATABASE_UNAVAILABLE, s"Database seems to be unavailable, retrying connection.")
}