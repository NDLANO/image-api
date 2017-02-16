/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.io.InputStream
import java.net.URL

import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties.StorageName
import no.ndla.imageapi.integration.AmazonClient
import no.ndla.imageapi.model.domain.Image

import scala.util.Try

trait ImageStorageService {
  this: AmazonClient =>
  val imageStorage: AmazonImageStorageService

  class AmazonImageStorageService extends LazyLogging {

    def get(imageKey: String): Option[(String, InputStream)] = {
      try {
        val s3Object = amazonClient.getObject(new GetObjectRequest(StorageName, imageKey))
        Option(
          s3Object.getObjectMetadata.getContentType,
          s3Object.getObjectContent)
      } catch {
        case e: Exception => {
          logger.warn("Could not get image with key {}", imageKey, e)
          None
        }
      }
    }

    def uploadFromUrl(image: Image, storageKey: String, urlOfImage: String): Try[_] = {
      uploadFromStream(new URL(urlOfImage).openStream(), storageKey, image.contentType, image.size)
    }

    def uploadFromStream(stream: InputStream, storageKey: String, contentType: String, size: Long): Try[String] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(size)

      Try(amazonClient.putObject(new PutObjectRequest(StorageName, storageKey, stream, metadata))).map(_ => storageKey)
    }

    def objectExists(storageKey: String): Boolean = {
      Try(amazonClient.doesObjectExist(StorageName, storageKey)).getOrElse(false)
    }

    def deleteObject(storageKey: String): Try[_] = Try(amazonClient.deleteObject(StorageName, storageKey))

    def createBucket: Bucket =  amazonClient.createBucket(new CreateBucketRequest(StorageName))

    def bucketExists: Boolean = amazonClient.doesBucketExist(StorageName)
  }
}
