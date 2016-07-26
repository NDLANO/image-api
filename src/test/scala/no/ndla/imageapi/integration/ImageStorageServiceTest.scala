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
import no.ndla.imageapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._

class ImageStorageServiceTest extends UnitSuite with TestEnvironment {

  val ImageStorageName = "TestBucket"
  val ImageWithNoThumb = TestData.nonexistingWithoutThumb
  val ImageWithThumb = TestData.nonexisting
  val Content = "content"
  val ContentType = "image/jpeg"

  override def beforeEach() = {
    reset(amazonClient)
  }

  test("That AmazonImageStorage.exists returns true when bucket exists") {
    when(amazonClient.doesBucketExist(ImageStorageName)).thenReturn(true)
    assert(imageStorage.exists())
  }

  test("That AmazonImageStorage.exists returns false when bucket does not exist") {
    when(amazonClient.doesBucketExist(ImageStorageName)).thenReturn(false)
    assert(imageStorage.exists() == false)
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
    assert(imageStorage.contains("nonExistingKey") == false)
  }

  test("That AmazonImageStorage.get returns a tuple with contenttype and data when the key exists") {
    val s3object = new S3Object()
    s3object.setObjectMetadata(new ObjectMetadata())
    s3object.getObjectMetadata().setContentType(ContentType)
    s3object.setObjectContent(new ByteArrayInputStream(Content.getBytes()))
    when(amazonClient.getObject(any[GetObjectRequest])).thenReturn(s3object)

    val image = imageStorage.get("existing")
    assert(image.isDefined)
    assert(image.get._1 == ContentType)
    assert(scala.io.Source.fromInputStream(image.get._2).mkString == Content)
  }

  test("That AmazonImageStorage.get returns None when the key does not exist") {
    when(amazonClient.getObject(any[GetObjectRequest])).thenThrow(new RuntimeException("Exception"))
    assert(imageStorage.get("nonexisting").isEmpty)
  }

  test("That AmazonImageStorage.upload uploads both thumb and image when both defined") {
    imageStorage.upload(ImageWithThumb, "test")
    verify(amazonClient, times(2)).putObject(any[PutObjectRequest])
  }

  test("That AmazonImageStorage.upload only uploads image when thumb is not defined") {
    imageStorage.upload(ImageWithNoThumb, "test")
    verify(amazonClient, times(1)).putObject(any[PutObjectRequest])
  }

}
