package no.ndla.imageapi.integration

import java.io.ByteArrayInputStream

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest, S3Object}
import model.Image
import no.ndla.imageapi.{TestData, UnitSpec}
import no.ndla.imageapi.business.ImageBucket
import org.mockito.Matchers._
import org.mockito.Mockito._

class AmazonImageBucketTest extends UnitSpec{

  val ImageBucketName = "TestBucket"
  val ImageWithNoThumb = TestData.nonexistingWithoutThumb
  val ImageWithThumb = TestData.nonexisting
  val Content = "content"
  val ContentType = "image/jpeg"

  var imageBucket: ImageBucket = _
  var s3ClientMock: AmazonS3Client = _

  override def beforeEach() = {
    s3ClientMock = mock[AmazonS3Client]
    imageBucket = new AmazonImageBucket(ImageBucketName, s3ClientMock)
  }

  "AmazonImageBucket.exists" should "return true when bucket exists" in {
    when(s3ClientMock.doesBucketExist(ImageBucketName)).thenReturn(true)
    assert(imageBucket.exists())
  }

  it should "return false when bucket does not exist" in {
    when(s3ClientMock.doesBucketExist(ImageBucketName)).thenReturn(false)
    assert(imageBucket.exists() == false)
  }

  "AmazonImageBucket.contains" should "return true when image exists" in {
    val s3ObjectMock = mock[S3Object]
    when(s3ClientMock.getObject(any[GetObjectRequest])).thenReturn(s3ObjectMock)
    assert(imageBucket.contains(ImageWithThumb))
  }

  it should "return false when image does not exist" in {
    val ase = new AmazonServiceException("Exception")
    ase.setErrorCode("NoSuchKey")
    when(s3ClientMock.getObject(any[GetObjectRequest])).thenThrow(ase)
    assert(imageBucket.contains(ImageWithThumb) == false)
  }

  "AmazonImageBucket.get" should "return a tuple with contenttype and data when the key exists" in {
    val s3object = new S3Object()
    s3object.setObjectMetadata(new ObjectMetadata())
    s3object.getObjectMetadata().setContentType(ContentType)
    s3object.setObjectContent(new ByteArrayInputStream(Content.getBytes()))
    when(s3ClientMock.getObject(any[GetObjectRequest])).thenReturn(s3object)

    val image = imageBucket.get("existing")
    assert(image.isDefined)
    assert(image.get._1 == ContentType)
    assert(scala.io.Source.fromInputStream(image.get._2).mkString == Content)
  }

  it should "return None when the key does not exist" in {
    when(s3ClientMock.getObject(any[GetObjectRequest])).thenThrow(new RuntimeException("Exception"))
    assert(imageBucket.get("nonexisting").isEmpty)
  }

  "AmazonImageBucket.upload" should "upload both thumb and image when both defined" in {
    imageBucket.upload(ImageWithThumb, "test")
    verify(s3ClientMock, times(2)).putObject(any[PutObjectRequest])
  }

  it should "upload only image when thumb is not defined" in {
    imageBucket.upload(ImageWithNoThumb, "test")
    verify(s3ClientMock, times(1)).putObject(any[PutObjectRequest])
  }

}
