package no.ndla.imageapi.model

import java.text.SimpleDateFormat
import java.util.Date


object Error {
  val GENERIC = "1"
  val NOT_FOUND = "2"
}

case class Error(code:String, description:String, occuredAt:String = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()))