/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, InputStream}

import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.LazyLogging
import javax.imageio.ImageIO
import no.ndla.imageapi.ImageApiProperties.StorageName
import no.ndla.imageapi.integration.AmazonClient
import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.model.domain.{Image, ImageStream}
import org.apache.commons.io.IOUtils
import scalaj.http.HttpRequest

import scala.util.{Failure, Success, Try}

trait ImageStorageService {
  this: AmazonClient =>
  val imageStorage: AmazonImageStorageService

  class AmazonImageStorageService extends LazyLogging {
    case class NdlaImage(s3Object: S3Object, fileName: String) extends ImageStream {
      lazy val imageContent = {
        val content = IOUtils.toByteArray(s3Object.getObjectContent)
        s3Object.getObjectContent.close()
        content
      }

      override val sourceImage: BufferedImage = ImageIO.read(stream)
      override def contentType: String = s3Object.getObjectMetadata.getContentType
      override def stream: InputStream = new ByteArrayInputStream(imageContent)
    }

    def get(imageKey: String): Try[ImageStream] = {
      Try(amazonClient.getObject(new GetObjectRequest(StorageName, imageKey))).map(s3Object =>
        NdlaImage(s3Object, imageKey)) match {
        case Success(e) => Success(e)
        case Failure(e) => Failure(new ImageNotFoundException(s"Image $imageKey does not exist"))
      }
    }

    def uploadFromUrl(image: Image, storageKey: String, request: HttpRequest): Try[String] =
      request.execute(stream => uploadFromStream(stream, storageKey, image.contentType, image.size)).body

    def uploadFromStream(stream: InputStream, storageKey: String, contentType: String, size: Long): Try[String] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(size)

      Try(amazonClient.putObject(new PutObjectRequest(StorageName, storageKey, stream, metadata))).map(_ => storageKey)
    }

    def objectExists(storageKey: String): Boolean = {
      Try(amazonClient.doesObjectExist(StorageName, storageKey)).getOrElse(false)
    }

    def objectSize(storageKey: String): Long = {
      Try(amazonClient.getObjectMetadata(StorageName, storageKey)).map(_.getContentLength).getOrElse(0)
    }

    def deleteObject(storageKey: String): Try[_] = Try(amazonClient.deleteObject(StorageName, storageKey))
  }

}
