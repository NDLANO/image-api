package setup

import java.io.File

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest, CreateBucketRequest}
import model.Image

class ImageBucket(ImageBucketName: String, LocalImageDirectory: String) {

  val S3 = new AmazonS3Client(new ProfileCredentialsProvider())
  S3.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def upload(image: Image) = {
    if(Option(image.thumbPath).isDefined){
      val thumbRequest: PutObjectRequest = new PutObjectRequest(ImageBucketName, image.thumbPath, new File(LocalImageDirectory + image.thumbPath))
      thumbRequest.setCannedAcl(CannedAccessControlList.PublicRead)
      val thumbResult = S3.putObject(thumbRequest)
    }

    val fullRequest: PutObjectRequest = new PutObjectRequest(ImageBucketName, image.imagePath, new File(LocalImageDirectory + image.imagePath))
    fullRequest.setCannedAcl(CannedAccessControlList.PublicRead)
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
