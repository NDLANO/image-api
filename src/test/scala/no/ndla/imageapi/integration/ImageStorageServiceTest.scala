/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest, S3Object}
import no.ndla.imageapi.TestData.NdlaLogoImage
import no.ndla.imageapi.{ImageApiProperties, TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._

class ImageStorageServiceTest extends UnitSuite with TestEnvironment {

  val ImageStorageName = ImageApiProperties.StorageName
  val ImageWithNoThumb = TestData.nonexistingWithoutThumb
  val Content = "content"
  val ContentType = "image/jpeg"
  override val imageStorage = new AmazonImageStorageService

  override def beforeEach() = {
    reset(amazonClient)
  }

  test("That AmazonImageStorage.exists returns true when bucket exists") {
    when(amazonClient.doesBucketExist(ImageStorageName)).thenReturn(true)
    assert(imageStorage.bucketExists)
  }

  test("That AmazonImageStorage.exists returns false when bucket does not exist") {
    when(amazonClient.doesBucketExist(ImageStorageName)).thenReturn(false)
    assert(!imageStorage.bucketExists)
  }

  test("That AmazonImageStorage.objectExists returns true when image exists") {
    when(amazonClient.doesObjectExist(any[String], any[String])).thenReturn(true)
    assert(imageStorage.objectExists("existingKey"))
  }

  test("That AmazonImageStorage.objectExists returns false when image does not exist") {
    when(amazonClient.doesObjectExist(any[String], any[String])).thenThrow(mock[AmazonServiceException])
    assert(!imageStorage.objectExists("nonExistingKey"))
  }

  test("That AmazonImageStorage.get returns a tuple with contenttype and data when the key exists") {
    val s3object = new S3Object()
    s3object.setObjectMetadata(new ObjectMetadata())
    s3object.getObjectMetadata.setContentType(ContentType)
    s3object.setObjectContent(NdlaLogoImage.stream)
    when(amazonClient.getObject(any[GetObjectRequest])).thenReturn(s3object)

    val image = imageStorage.get("existing")
    assert(image.isSuccess)
    assert(image.get.contentType == ContentType)
    assert(image.get.sourceImage != null)
  }

  test("That AmazonImageStorage.get returns None when the key does not exist") {
    when(amazonClient.getObject(any[GetObjectRequest])).thenThrow(new RuntimeException("Exception"))
    assert(imageStorage.get("nonexisting").isFailure)
  }

}
