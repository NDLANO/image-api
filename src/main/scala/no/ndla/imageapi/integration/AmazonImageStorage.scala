package no.ndla.imageapi.integration

import java.io.{File, InputStream}

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import model.ImageMetaInformation
import no.ndla.imageapi.business.ImageStorage

class AmazonImageStorage(imageStorageName: String, s3Client: AmazonS3Client) extends ImageStorage {

  def get(imageKey: String): Option[(String, InputStream)] = {
    try{
      val s3Object = s3Client.getObject(new GetObjectRequest(imageStorageName, imageKey))
      Option(
        s3Object.getObjectMetadata().getContentType,
        s3Object.getObjectContent)
    } catch {
      case ase: Exception => None
    }
  }

  def upload(imageMetaInformation: ImageMetaInformation, imageDirectory: String) = {
    imageMetaInformation.images.small.foreach(small => {
      val thumbResult = s3Client.putObject(new PutObjectRequest(imageStorageName, small.url, new File(imageDirectory + small.url)))
    })

    imageMetaInformation.images.full.foreach(full => {
      s3Client.putObject(new PutObjectRequest(imageStorageName, full.url, new File(imageDirectory + full.url)))
    })

  }

  def contains(imageMetaInformation: ImageMetaInformation): Boolean = {
    imageMetaInformation.images.full match {
      case Some(full) => {
        try {
          Option(s3Client.getObject(new GetObjectRequest(imageStorageName, full.url))).isDefined
        } catch {
          case ase: AmazonServiceException => if (ase.getErrorCode == "NoSuchKey") false else throw ase
        }
      }
      case None => false
    }
  }

  def create() = {
    s3Client.createBucket(new CreateBucketRequest(imageStorageName))
  }

  def exists(): Boolean = {
    s3Client.doesBucketExist(imageStorageName)
  }
}
