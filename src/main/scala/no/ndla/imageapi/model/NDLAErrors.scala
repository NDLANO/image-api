/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model

import java.text.SimpleDateFormat
import java.util.Date
import io.searchbox.client.JestResult
import no.ndla.imageapi.ImageApiProperties

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT FOUND"
  val INDEX_MISSING = "INDEX MISSING"
  val VALIDATION = "VALIDATION"
  val FILE_TOO_BIG = "FILE TOO BIG"

  val GenericError = Error(GENERIC, s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
  val IndexMissingError = Error(INDEX_MISSING, s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
  val FileTooBigError = Error(FILE_TOO_BIG, s"The file is too big. Max file size is ${ImageApiProperties.MaxImageFileSizeBytes / 1024 / 1024} MiB")
  val ImageNotFoundError = Error(NOT_FOUND, s"Ooops. That image does not exists")
}

case class Error(code:String, description:String, occuredAt:String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
class ImageNotFoundException(message: String) extends RuntimeException(message)
class ValidationException(message: String = "Validation error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)
case class ValidationMessage(field: String, message: String)
class NdlaSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}
