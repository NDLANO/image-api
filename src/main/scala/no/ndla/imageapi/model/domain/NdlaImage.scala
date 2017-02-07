package no.ndla.imageapi.model.domain

import java.io.InputStream

import com.amazonaws.services.s3.model.S3Object

trait ImageStream {
  def contentType: String
  def stream: InputStream
  def fileName: String
  def format: String = fileName.substring(fileName.lastIndexOf(".") + 1)
}

case class NdlaImage(s3Object: S3Object, fileName: String) extends ImageStream {
  override def contentType: String = s3Object.getObjectMetadata.getContentType
  override def stream: InputStream = s3Object.getObjectContent
}
