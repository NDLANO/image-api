/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.model

import java.text.SimpleDateFormat
import java.util.Date

import no.ndla.imageapi.ImageApiProperties


object Error {
  val GENERIC = "1"
  val NOT_FOUND = "2"
  val INDEX_MISSING = "3"

  val GenericError = Error(GENERIC, s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
  val IndexMissingError = Error(INDEX_MISSING, s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ImageApiProperties.ContactEmail} if the error persists.")
}

case class Error(code:String, description:String, occuredAt:String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))
class ImageNotFoundException(message: String) extends RuntimeException(message)
