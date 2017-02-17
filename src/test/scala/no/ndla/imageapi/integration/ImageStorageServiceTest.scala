/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import java.io.ByteArrayInputStream

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest, S3Object}
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
    assert(imageStorage.exists())
  }

  test("That AmazonImageStorage.exists returns false when bucket does not exist") {
    when(amazonClient.doesBucketExist(ImageStorageName)).thenReturn(false)
    assert(!imageStorage.exists())
  }

  test("That AmazonImageStorage.contains returns true when image exists") {
    val s3ObjectMock = mock[S3Object]
    when(amazonClient.getObject(any[GetObjectRequest])).thenReturn(s3ObjectMock)
    assert(imageStorage.contains("existingKey"))
  }

  test("That AmazonImageStorage.contains returns false when image does not exist") {
    val ase = new AmazonServiceException("Exception")
    ase.setErrorCode("NoSuchKey")
    when(amazonClient.getObject(any[GetObjectRequest])).thenThrow(ase)
    assert(!imageStorage.contains("nonExistingKey"))
  }

  test("That AmazonImageStorage.get returns a tuple with contenttype and data when the key exists") {
    val s3object = new S3Object()
    s3object.setObjectMetadata(new ObjectMetadata())
    s3object.getObjectMetadata.setContentType(ContentType)
    s3object.setObjectContent(new ByteArrayInputStream(Content.getBytes()))
    when(amazonClient.getObject(any[GetObjectRequest])).thenReturn(s3object)

    val image = imageStorage.get("existing")
    assert(image.isSuccess)
    assert(image.get.contentType == ContentType)
    assert(scala.io.Source.fromInputStream(image.get.stream).mkString == Content)
  }

  test("That AmazonImageStorage.get returns None when the key does not exist") {
    when(amazonClient.getObject(any[GetObjectRequest])).thenThrow(new RuntimeException("Exception"))
    assert(imageStorage.get("nonexisting").isFailure)
  }

  test("That AmazonImageStorage.upload only uploads image when thumb is not defined") {
    imageStorage.upload(ImageWithNoThumb, "test")
    verify(amazonClient, times(1)).putObject(any[PutObjectRequest])
  }

}
