/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.io.InputStream
import java.util.Date
import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.api._
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.domain.{Image, ImageMetaInformation}
import no.ndla.imageapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl
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

  val newImageMeta = NewImageMetaInformationV2(
    "title",
    "alt text",
    Copyright(License("by", "", None), "", Seq.empty, Seq.empty, Seq.empty, None, None),
    Seq.empty,
    "",
    "en"
  )

  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val domainImageMeta = converterService.asDomainImageMetaInformationV2(newImageMeta, Image(newFileName, 1024, "image/jpeg"))

  override def beforeEach = {
    when(fileMock1.getContentType).thenReturn(Some("image/jpeg"))
    when(fileMock1.get).thenReturn(Array[Byte](-1, -40, -1))
    when(fileMock1.size).thenReturn(1024)
    when(fileMock1.name).thenReturn("file.jpg")

    val applicationUrl = mock[HttpServletRequest]
    when(applicationUrl.getHeader(any[String])).thenReturn("http")
    when(applicationUrl.getServerName).thenReturn("localhost")
    when(applicationUrl.getServletPath).thenReturn("/image-api/v2/images/")
    ApplicationUrl.set(applicationUrl)

    reset(imageRepository, indexService, imageStorage)
    when(imageRepository.insert(any[ImageMetaInformation])(any[DBSession])).thenReturn(domainImageMeta.copy(id=Some(1)))
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
    verify(imageRepository, times(0)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(indexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to insert into database") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation])).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long])).thenReturn(Success(newFileName))
    when(imageRepository.insert(any[ImageMetaInformation])(any[DBSession])).thenThrow(new RuntimeException)

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
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
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

    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
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
    val domain = converterService.asDomainImageMetaInformationV2(newImageMeta, Image(newFileName, 1024, "image/jpeg"))
    domain.updatedBy should equal ("ndla54321")
    domain.updated should equal(updated())
  }

  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing = Seq(domain.ImageTitle("Tittel 1", "nb"), domain.ImageTitle("Tittel 2", "nn"), domain.ImageTitle("Tittel 3", "unknown"))
    writeService.mergeLanguageFields(existing, Seq()) should equal(existing)
  }

  test("That mergeLanguageFields updated the english title only when specified") {
    val tittel1 = domain.ImageTitle("Tittel 1", "nb")
    val tittel2 = domain.ImageTitle("Tittel 2", "nn")
    val tittel3 = domain.ImageTitle("Tittel 3", "en")
    val oppdatertTittel3 = domain.ImageTitle("Title 3 in english", "en")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel3)

    writeService.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel2, oppdatertTittel3))
  }

  test("That mergeLanguageFields removes a title that is empty") {
    val tittel1 = domain.ImageTitle("Tittel 1", "nb")
    val tittel2 = domain.ImageTitle("Tittel 2", "nn")
    val tittel3 = domain.ImageTitle("Tittel 3", "en")
    val tittelToRemove = domain.ImageTitle("", "nn")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(tittelToRemove)

    writeService.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3))
  }

  test("That mergeLanguageFields updates the title with unknown language specified") {
    val tittel1 = domain.ImageTitle("Tittel 1", "nb")
    val tittel2 = domain.ImageTitle("Tittel 2", "unknown")
    val tittel3 = domain.ImageTitle("Tittel 3", "en")
    val oppdatertTittel2 = domain.ImageTitle("Tittel 2 er oppdatert", "unknown")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel2)

    writeService.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct content") {
    val desc1 = domain.ImageAltText("Beskrivelse 1", "nb")
    val desc2 = domain.ImageAltText("Beskrivelse 2", "unknown")
    val desc3 = domain.ImageAltText("Beskrivelse 3", "en")
    val oppdatertDesc2 = domain.ImageAltText("Beskrivelse 2 er oppdatert", "unknown")

    val existing = Seq(desc1, desc2, desc3)
    val updated = Seq(oppdatertDesc2)

    writeService.mergeLanguageFields(existing, updated) should equal(Seq(desc1, desc3, oppdatertDesc2))
  }

  test("mergeImages should append a new language if language not already exists") {
    val date = new Date()
    val user = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy=user )
    val toUpdate = UpdateImageMetaInformation(
      "en",
      Some("Title"),
      Some("AltText"),
      None,
      None,
      None
    )

    val expectedResult = existing.copy(
      titles = List(existing.titles.head, domain.ImageTitle("Title", "en")),
      alttexts = List(existing.alttexts.head, domain.ImageAltText("AltText", "en"))
    )

    when(authUser.id()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("mergeImages overwrite a languages if specified language already exist in cover") {
    val date = new Date()
    val user = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy=user )
    val toUpdate = UpdateImageMetaInformation(
      "nb",
      Some("Title"),
      Some("AltText"),
      None,
      None,
      None
    )

    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb"))
    )

    when(authUser.id()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("mergeImages updates optional values if specified") {
    val date = new Date()
    val user = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy=user )
    val toUpdate = UpdateImageMetaInformation(
      "nb",
      Some("Title"),
      Some("AltText"),
      Some(Copyright(License("testLic", "License for testing", None), "test", List(Author("Opphavsmann", "Testerud")), List(), List(), None, None)),
      Some(List("a", "b", "c")),
      Some("Caption")
    )

    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb")),
      copyright = domain.Copyright(domain.License("testLic", "License for testing", None), "test", List(domain.Author("Opphavsmann", "Testerud")), None, None),
      tags = List(domain.ImageTag(List("a", "b", "c"), "nb")),
      captions = List(domain.ImageCaption("Caption", "nb"))
    )

    when(authUser.id()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }
}
