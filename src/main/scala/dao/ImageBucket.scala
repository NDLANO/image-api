package dao

import java.io.{File, InputStream}

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import model.Image

class ImageBucket(ImageBucketName: String = "ndla-image") {

  val S3 = new AmazonS3Client(new ProfileCredentialsProvider())
  S3.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def get(imageKey: String): Option[(String, InputStream)] = {
    try{
      val s3Object = S3.getObject(new GetObjectRequest(ImageBucketName, imageKey))
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
      val thumbResult = S3.putObject(thumbRequest)
    }

    val fullRequest: PutObjectRequest = new PutObjectRequest(ImageBucketName, image.imagePath, new File(imageDirectory + image.imagePath))
    S3.putObject(fullRequest)
  }

  def contains(image: Image): Boolean = {
    // TODO: Hvordan sjekke om bildet allerede eksisterer?
    false
  }

  def create() = {
    val bucket = S3.createBucket(new CreateBucketRequest(ImageBucketName))
  }

  def exists(): Boolean = {
    S3.doesBucketExist(ImageBucketName)
  }

}
