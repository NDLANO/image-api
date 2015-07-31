package no.ndla.imageapi.integration

import java.io.{File, InputStream}

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import model.Image
import no.ndla.imageapi.business.ImageBucket

class AmazonImageBucket(imageBucketName: String, s3Client: AmazonS3Client) extends ImageBucket {

  def get(imageKey: String): Option[(String, InputStream)] = {
    try{
      val s3Object = s3Client.getObject(new GetObjectRequest(imageBucketName, imageKey))
      Option(
        s3Object.getObjectMetadata().getContentType,
        s3Object.getObjectContent)
    } catch {
      case ase: Exception => None
    }
  }

  def upload(image: Image, imageDirectory: String) = {
    if(Option(image.thumbPath).isDefined){
      val thumbRequest: PutObjectRequest = new PutObjectRequest(imageBucketName, image.thumbPath, new File(imageDirectory + image.thumbPath))
      val thumbResult = s3Client.putObject(thumbRequest)
    }

    val fullRequest: PutObjectRequest = new PutObjectRequest(imageBucketName, image.imagePath, new File(imageDirectory + image.imagePath))
    s3Client.putObject(fullRequest)
  }

  def contains(image: Image): Boolean = {
    try {
      Option(s3Client.getObject(new GetObjectRequest(imageBucketName, image.imagePath))).isDefined
    } catch {
      case ase: AmazonServiceException => if (ase.getErrorCode == "NoSuchKey") false else throw ase
    }
  }

  def create() = {
    s3Client.createBucket(new CreateBucketRequest(imageBucketName))
  }

  def exists(): Boolean = {
    s3Client.doesBucketExist(imageBucketName)
  }
}
