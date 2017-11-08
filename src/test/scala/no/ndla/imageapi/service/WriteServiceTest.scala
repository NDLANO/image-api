/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.io.InputStream
import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.api._
import no.ndla.imageapi.model.domain.{Image, ImageMetaInformation}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import io.digitallibrary.network.ApplicationUrl
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatra.servlet.FileItem
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val writeService = new WriteService
  override val converterService = new ConverterService
  val newFileName = "AbCdeF.mp3"
  val fileMock1: FileItem = mock[FileItem]

  val newImageMeta = NewImageMetaInformation(
    None,
    Seq(ImageTitle("title", "en")),
    Seq(ImageAltText("alt text", "en")),
    Copyright(License("by", "", None), "", Seq.empty),
    None,
    None
  )

  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val domainImageMeta = converterService.asDomainImageMetaInformation(newImageMeta, Image(newFileName, 1024, "image/jpeg"))

  override def beforeEach = {
    when(fileMock1.getContentType).thenReturn(Some("image/jpeg"))
    when(fileMock1.get).thenReturn(Array[Byte](-1, -40, -1))
    when(fileMock1.size).thenReturn(1024)
    when(fileMock1.name).thenReturn("file.jpg")

    val applicationUrl = mock[HttpServletRequest]
    when(applicationUrl.getHeader(any[String])).thenReturn("http")
    when(applicationUrl.getServerName).thenReturn("localhost")
    when(applicationUrl.getServletPath).thenReturn("/image-api/v1/images/")
    ApplicationUrl.set(applicationUrl)

    reset(imageRepository, indexService, imageStorage)
    when(imageRepository.insert(any[ImageMetaInformation], any[Option[String]])(any[DBSession])).thenReturn(domainImageMeta.copy(id=Some(1)))
  }

  test("randomFileName should return a random filename with a given length and extension") {
    val extension = ".jpg"

    val result = writeService.randomFileName(extension)
    result.length should be (12)
    result.endsWith(extension) should be (true)

    val resultWithNegativeLength = writeService.randomFileName(extension, -1)
    resultWithNegativeLength.length should be (1 + extension.length)
    resultWithNegativeLength.endsWith(extension) should be (true)
  }

  test("uploadFile should return Success if file upload succeeds") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Success(newFileName))
    val result = writeService.uploadImage(fileMock1)
    verify(imageStorage, times(1)).uploadFromStream(any[InputStream], any[String], any[String], any[Long])

    result should equal(Success(Image(newFileName, 1024, "image/jpeg")))
  }

  test("uploadFile should return Failure if file upload failed") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Failure(new RuntimeException))

    writeService.uploadImage(fileMock1).isFailure should be (true)
  }

  test("storeNewImage should return Failure if upload failes") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Failure(new RuntimeException))

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be (true)
  }

  test("storeNewImage should return Failure if validation fails") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation])).thenReturn(Failure(new ValidationException(errors=Seq())))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Success(newFileName))

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be (true)
    verify(imageRepository, times(0)).insert(any[ImageMetaInformation], any[Option[String]])(any[DBSession])
    verify(indexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to insert into database") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation])).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Success(newFileName))
    when(imageRepository.insert(any[ImageMetaInformation], any[Option[String]])(any[DBSession])).thenThrow(new RuntimeException)

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be (true)
    verify(indexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to index image metadata") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation])).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Success(newFileName))
    when(indexService.indexDocument(any[ImageMetaInformation])).thenReturn(Failure(new RuntimeException))

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be (true)
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation], any[Option[String]])(any[DBSession])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Success if creation of new image file succeeded") {
    val afterInsert = domainImageMeta.copy(id=Some(1))
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation])).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Success(newFileName))
    when(indexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))

    val result = writeService.storeNewImage(newImageMeta, fileMock1)
    result.isSuccess should be (true)
    result should equal(Success(afterInsert))

    verify(imageRepository, times(1)).insert(any[ImageMetaInformation], any[Option[String]])(any[DBSession])
    verify(indexService, times(1)).indexDocument(any[ImageMetaInformation])
  }

  test("getFileExtension returns the extension") {
    writeService.getFileExtension("image.jpg") should equal(Some(".jpg"))
    writeService.getFileExtension("ima.ge.jpg") should equal(Some(".jpg"))
    writeService.getFileExtension(".jpeg") should equal(Some(".jpeg"))
  }

  test("getFileExtension returns None if no extension was found") {
    writeService.getFileExtension("image-jpg") should equal(None)
    writeService.getFileExtension("jpeg") should equal(None)
  }

  test("converter to domain should set updatedBy from authUser and updated date"){
    when(authUser.id()).thenReturn("ndla54321")
    when(clock.now()).thenReturn(updated())
    val domain = converterService.asDomainImageMetaInformation(newImageMeta, Image(newFileName, 1024, "image/jpeg"))
    domain.updatedBy should equal ("ndla54321")
    domain.updated should equal(updated())
  }

}
