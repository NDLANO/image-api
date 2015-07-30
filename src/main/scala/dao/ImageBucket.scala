package dao

import java.io.{File, InputStream}

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import model.Image

object ImageBucket {
  val ImageBucketName = "ndla-image"

  def get(imageKey: String): Option[(String, InputStream)] = {
    try{
      val s3Object = s3Client.getObject(new GetObjectRequest(ImageBucketName, imageKey))
      Option(
        s3Object.getObjectMetadata().getContentType,
        s3Object.getObjectContent)
    } catch {
      case ase: Exception => None
    }
  }

  def upload(image: Image, imageDirectory: String) = {
    if(Option(image.thumbPath).isDefined){
      val thumbRequest: PutObjectRequest = new PutObjectRequest(ImageBucketName, image.thumbPath, new File(imageDirectory + image.thumbPath))
      val thumbResult = s3Client.putObject(thumbRequest)
    }

    val fullRequest: PutObjectRequest = new PutObjectRequest(ImageBucketName, image.imagePath, new File(imageDirectory + image.imagePath))
    s3Client.putObject(fullRequest)
  }

  def contains(image: Image): Boolean = {
    try {
      Option(s3Client.getObject(new GetObjectRequest(ImageBucketName, image.imagePath))).isDefined
    } catch {
      case ase: AmazonServiceException => if (ase.getErrorCode == "NoSuchKey") false else throw ase
    }
  }

  def create() = {
    s3Client.createBucket(new CreateBucketRequest(ImageBucketName))
  }

  def exists(): Boolean = {
    s3Client.doesBucketExist(ImageBucketName)
  }

  def s3Client(): AmazonS3Client = {
    val s3Client = new AmazonS3Client(new ProfileCredentialsProvider())
    s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    s3Client
  }

}
