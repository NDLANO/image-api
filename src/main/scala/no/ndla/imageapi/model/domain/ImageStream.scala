package no.ndla.imageapi.model.domain

import java.awt.image.BufferedImage
import java.io.InputStream

trait ImageStream {
  def contentType: String
  def stream: InputStream
  def fileName: String
  def format: String = fileName.substring(fileName.lastIndexOf(".") + 1)
  val sourceImage: BufferedImage
  def copyWithNewContentType(contentType: String): ImageStream
}
