/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model

import io.searchbox.client.JestResult
import no.ndla.imageapi.ImageApiProperties


class ImageNotFoundException(message: String) extends RuntimeException(message)

class AccessDeniedException(message: String) extends RuntimeException(message)

class ImportException(message: String) extends RuntimeException(message)

class ValidationException(message: String = "Validation error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)

case class ValidationMessage(field: String, message: String)

class NdlaSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}

class S3UploadException(message: String) extends RuntimeException(message)

class ResultWindowTooLargeException(message: String) extends RuntimeException(message)

