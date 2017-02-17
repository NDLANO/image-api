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
  val GENERIC = "1"
  val NOT_FOUND = "2"
  val INDEX_MISSING = "3"
  val VALIDATION = "4"

  val GenericError = api.Error(GENERIC, s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
  val IndexMissingError = api.Error(INDEX_MISSING, s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
}

class ImageNotFoundException(message: String) extends RuntimeException(message)
class ValidationException(message: String) extends RuntimeException(message)
class NdlaSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}
