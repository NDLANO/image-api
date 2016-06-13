/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.service

import java.io.{ByteArrayInputStream, File, InputStream}
import java.net.URL

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration.AmazonClientComponent
import no.ndla.imageapi.model.{Image, ImageMetaInformation}

trait ImageStorageService {
  this: AmazonClientComponent =>
  val imageStorage: AmazonImageStorageService

  class AmazonImageStorageService extends LazyLogging {

    def get(imageKey: String): Option[(String, InputStream)] = {
      try {
        val s3Object = amazonClient.getObject(new GetObjectRequest(storageName, imageKey))
        Option(
          s3Object.getObjectMetadata().getContentType,
          s3Object.getObjectContent)
      } catch {
        case e: Exception => {
          logger.warn("Could not get image with key {}", imageKey, e)
          None
        }
      }
    }

    def upload(imageMetaInformation: ImageMetaInformation, imageDirectory: String) = {
      imageMetaInformation.images.small.foreach(small => {
        val thumbResult = amazonClient.putObject(new PutObjectRequest(storageName, small.url, new File(imageDirectory + small.url)))
      })

      imageMetaInformation.images.full.foreach(full => {
        amazonClient.putObject(new PutObjectRequest(storageName, full.url, new File(imageDirectory + full.url)))
      })

    }

    def uploadFromByteArray(image: Image, storageKey: String, bytes: Array[Byte]): Unit = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(image.contentType)
      metadata.setContentLength(image.size.toLong)

      val request = new PutObjectRequest(storageName, storageKey, new ByteArrayInputStream(bytes), metadata)
      val putResult = amazonClient.putObject(request)
    }

    def uploadFromUrl(image: Image, storageKey: String, urlOfImage: String): Unit = {
      val imageStream = new URL(urlOfImage).openStream()
      val metadata = new ObjectMetadata()
      metadata.setContentType(image.contentType)
      metadata.setContentLength(image.size.toLong)

      val request = new PutObjectRequest(storageName, storageKey, imageStream, metadata)
      val putResult = amazonClient.putObject(request)
    }

    def contains(storageKey: String): Boolean = {
      try {
        val s3Object = Option(amazonClient.getObject(new GetObjectRequest(storageName, storageKey)))
        s3Object match {
          case Some(obj) => {
            obj.close()
            true
          }
          case None => false
        }
      } catch {
        case ase: AmazonServiceException => if (ase.getErrorCode == "NoSuchKey") false else throw ase
      }
    }

    def create() = {
      amazonClient.createBucket(new CreateBucketRequest(storageName))
    }

    def exists(): Boolean = {
      amazonClient.doesBucketExist(storageName)
    }
  }
}
