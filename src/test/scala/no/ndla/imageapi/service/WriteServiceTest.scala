/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.TestData.DiskImage

import java.io.InputStream
import java.util.Date
import javax.servlet.http.HttpServletRequest
import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.api._
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.domain.{Image, ImageMetaInformation, ModelReleasedStatus}
import no.ndla.imageapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatra.servlet.FileItem
import scalikejdbc.DBSession

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val writeService = new WriteService
  override val converterService = new ConverterService
  val newFileName = "AbCdeF.mp3"
  val fileMock1: FileItem = mock[FileItem]
  val NdlaLogoImage = DiskImage("ndla_logo.jpg")

  val newImageMeta = NewImageMetaInformationV2(
    "title",
    "alt text",
    Copyright(License("by", "", None), "", Seq.empty, Seq.empty, Seq.empty, None, None, None),
    Seq.empty,
    "",
    "en",
    Some(ModelReleasedStatus.YES.toString)
  )

  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val domainImageMeta =
    converterService.asDomainImageMetaInformationV2(newImageMeta, Image(newFileName, 1024, "image/jpeg", 50, 50)).get

  val multiLangImage = domain.ImageMetaInformation(
    Some(2),
    List(domain.ImageTitle("nynorsk", "nn"), domain.ImageTitle("english", "en"), domain.ImageTitle("norsk", "und")),
    List(),
    "yolo.jpeg",
    100,
    "image/jpeg",
    domain.Copyright("", "", List(), List(), List(), None, None, None),
    List(),
    List(),
    "ndla124",
    updated(),
    updated(),
    "ndla124",
    ModelReleasedStatus.YES,
    Seq.empty,
    50,
    50
  )

  override def beforeEach() = {
    when(fileMock1.getContentType).thenReturn(Some("image/jpeg"))
    when(fileMock1.get()).thenReturn(Array[Byte](-1, -40, -1))
    when(fileMock1.size).thenReturn(1024)
    when(fileMock1.name).thenReturn("file.jpg")

    val applicationUrl = mock[HttpServletRequest]
    when(applicationUrl.getHeader(any[String])).thenReturn("http")
    when(applicationUrl.getServerName).thenReturn("localhost")
    when(applicationUrl.getServletPath).thenReturn("/image-api/v2/images/")
    ApplicationUrl.set(applicationUrl)

    reset(imageRepository, imageIndexService, imageStorage, tagIndexService)
    when(imageRepository.insert(any[ImageMetaInformation])(any[DBSession]))
      .thenReturn(domainImageMeta.copy(id = Some(1)))
  }

  test("randomFileName should return a random filename with a given length and extension") {
    val extension = ".jpg"

    val result = writeService.randomFileName(extension)
    result.length should be(12)
    result.endsWith(extension) should be(true)

    val resultWithNegativeLength = writeService.randomFileName(extension, -1)
    resultWithNegativeLength.length should be(1 + extension.length)
    resultWithNegativeLength.endsWith(extension) should be(true)
  }

  test("uploadFile should return Success if file upload succeeds") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(fileMock1.getInputStream).thenReturn(NdlaLogoImage.stream)
    val result = writeService.uploadImage(fileMock1)
    verify(imageStorage, times(1)).uploadFromStream(any[InputStream], any[String], any[String], any[Long])

    result should equal(Success(Image(newFileName, 1024, "image/jpeg", 189, 60)))
  }

  test("uploadFile should return Failure if file upload failed") {
    when(imageStorage.objectExists(any[String])).thenReturn(false)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Failure(new RuntimeException))

    writeService.uploadImage(fileMock1).isFailure should be(true)
  }

  test("storeNewImage should return Failure if upload failes") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Failure(new RuntimeException))

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
  }

  test("storeNewImage should return Failure if validation fails") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None)))
      .thenReturn(Failure(new ValidationException(errors = Seq())))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(fileMock1.getInputStream).thenReturn(NdlaLogoImage.stream)

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageRepository, times(0)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(imageIndexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to insert into database") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageRepository.insert(any[ImageMetaInformation])(any[DBSession])).thenThrow(new RuntimeException)
    when(fileMock1.getInputStream).thenReturn(NdlaLogoImage.stream)

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageIndexService, times(0)).indexDocument(any[ImageMetaInformation])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to index image metadata") {
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Failure(new RuntimeException))
    when(fileMock1.getInputStream).thenReturn(NdlaLogoImage.stream)

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(imageStorage, times(1)).deleteObject(any[String])
  }

  test("storeNewImage should return Failure if failed to index tag metadata") {
    val afterInsert = domainImageMeta.copy(id = Some(1))
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Failure(new RuntimeException))
    when(fileMock1.getInputStream).thenReturn(NdlaLogoImage.stream)

    writeService.storeNewImage(newImageMeta, fileMock1).isFailure should be(true)
    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(imageStorage, times(1)).deleteObject(any[String])
    verify(imageIndexService, times(1)).deleteDocument(eqTo(1))
  }

  test("storeNewImage should return Success if creation of new image file succeeded") {
    val afterInsert = domainImageMeta.copy(id = Some(1))
    when(validationService.validateImageFile(any[FileItem])).thenReturn(None)
    when(validationService.validate(any[ImageMetaInformation], eqTo(None))).thenReturn(Success(domainImageMeta))
    when(imageStorage.uploadFromStream(any[InputStream], any[String], any[String], any[Long]))
      .thenReturn(Success(newFileName))
    when(imageIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))
    when(tagIndexService.indexDocument(any[ImageMetaInformation])).thenReturn(Success(afterInsert))
    when(fileMock1.getInputStream).thenReturn(NdlaLogoImage.stream)

    val result = writeService.storeNewImage(newImageMeta, fileMock1)
    result.isSuccess should be(true)
    result should equal(Success(afterInsert))

    verify(imageRepository, times(1)).insert(any[ImageMetaInformation])(any[DBSession])
    verify(imageIndexService, times(1)).indexDocument(any[ImageMetaInformation])
    verify(tagIndexService, times(1)).indexDocument(any[ImageMetaInformation])
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

  test("converter to domain should set updatedBy from authUser and updated date") {
    when(authUser.userOrClientid()).thenReturn("ndla54321")
    when(clock.now()).thenReturn(updated())
    val domain =
      converterService.asDomainImageMetaInformationV2(newImageMeta, Image(newFileName, 1024, "image/jpeg", 50, 50)).get
    domain.updatedBy should equal("ndla54321")
    domain.updated should equal(updated())
  }

  test("That mergeLanguageFields returns original list when updated is empty") {
    val existing = Seq(domain.ImageTitle("Tittel 1", "nb"),
                       domain.ImageTitle("Tittel 2", "nn"),
                       domain.ImageTitle("Tittel 3", "und"))
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
    val tittel2 = domain.ImageTitle("Tittel 2", "und")
    val tittel3 = domain.ImageTitle("Tittel 3", "en")
    val oppdatertTittel2 = domain.ImageTitle("Tittel 2 er oppdatert", "und")

    val existing = Seq(tittel1, tittel2, tittel3)
    val updated = Seq(oppdatertTittel2)

    writeService.mergeLanguageFields(existing, updated) should equal(Seq(tittel1, tittel3, oppdatertTittel2))
  }

  test("That mergeLanguageFields also updates the correct content") {
    val desc1 = domain.ImageAltText("Beskrivelse 1", "nb")
    val desc2 = domain.ImageAltText("Beskrivelse 2", "und")
    val desc3 = domain.ImageAltText("Beskrivelse 3", "en")
    val oppdatertDesc2 = domain.ImageAltText("Beskrivelse 2 er oppdatert", "und")

    val existing = Seq(desc1, desc2, desc3)
    val updated = Seq(oppdatertDesc2)

    writeService.mergeLanguageFields(existing, updated) should equal(Seq(desc1, desc3, oppdatertDesc2))
  }

  test("mergeImages should append a new language if language not already exists") {
    val date = new Date()
    val user = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate = UpdateImageMetaInformation(
      "en",
      Some("Title"),
      Some("AltText"),
      None,
      None,
      None,
      None
    )

    val expectedResult = existing.copy(
      titles = List(existing.titles.head, domain.ImageTitle("Title", "en")),
      alttexts = List(existing.alttexts.head, domain.ImageAltText("AltText", "en")),
      editorNotes = Seq(domain.EditorNote(date, user, "Added new language 'en'."))
    )

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("mergeImages overwrite a languages if specified language already exist in cover") {
    val date = new Date()
    val user = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate = UpdateImageMetaInformation(
      "nb",
      Some("Title"),
      Some("AltText"),
      None,
      None,
      None,
      None
    )

    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb")),
      editorNotes = Seq(domain.EditorNote(date, user, "Updated image data."))
    )

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("mergeImages updates optional values if specified") {
    val date = new Date()
    val user = "ndla124"
    val existing = TestData.elg.copy(updated = date, updatedBy = user)
    val toUpdate = UpdateImageMetaInformation(
      "nb",
      Some("Title"),
      Some("AltText"),
      Some(
        Copyright(License("testLic", "License for testing", None),
                  "test",
                  List(Author("Opphavsmann", "Testerud")),
                  List(),
                  List(),
                  None,
                  None,
                  None)),
      Some(List("a", "b", "c")),
      Some("Caption"),
      Some(ModelReleasedStatus.NO.toString)
    )

    val expectedResult = existing.copy(
      titles = List(domain.ImageTitle("Title", "nb")),
      alttexts = List(domain.ImageAltText("AltText", "nb")),
      copyright = domain.Copyright("testLic",
                                   "test",
                                   List(domain.Author("Opphavsmann", "Testerud")),
                                   List(),
                                   List(),
                                   None,
                                   None,
                                   None),
      tags = List(domain.ImageTag(List("a", "b", "c"), "nb")),
      captions = List(domain.ImageCaption("Caption", "nb")),
      modelReleased = ModelReleasedStatus.NO,
      editorNotes = Seq(domain.EditorNote(date, "ndla124", "Updated image data."))
    )

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    writeService.mergeImages(existing, toUpdate) should equal(expectedResult)
  }

  test("that deleting image deletes database entry, s3 object, and indexed document") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val imageId = 4444.toLong

    when(imageRepository.withId(imageId)).thenReturn(Some(domainImageMeta))
    when(imageRepository.delete(eqTo(imageId))(any[DBSession])).thenReturn(1)
    when(imageStorage.deleteObject(any[String])).thenReturn(Success(()))
    when(imageIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Long](0)))
    when(tagIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Long](0)))

    writeService.deleteImageAndFiles(imageId)

    verify(imageStorage, times(1)).deleteObject(domainImageMeta.imageUrl)
    verify(imageIndexService, times(1)).deleteDocument(imageId)
    verify(tagIndexService, times(1)).deleteDocument(imageId)
    verify(imageRepository, times(1)).delete(eqTo(imageId))(any[DBSession])
  }

  test("That deleting language version deletes language") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val date = new Date()
    val user = "ndla124"

    when(authUser.userOrClientid()).thenReturn(user)
    when(clock.now()).thenReturn(date)

    val imageId = 5555.toLong
    val image = multiLangImage.copy(id = Some(imageId))
    val expectedImage =
      image.copy(
        titles = List(domain.ImageTitle("english", "en"), domain.ImageTitle("norsk", "und")),
        editorNotes = image.editorNotes :+ domain.EditorNote(date, user, "Deleted language 'nn'.")
      )

    when(imageRepository.withId(imageId)).thenReturn(Some(image))
    when(imageRepository.update(any[domain.ImageMetaInformation], eqTo(imageId))).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[domain.ImageMetaInformation](0))
    when(validationService.validate(any[domain.ImageMetaInformation], any[Option[domain.ImageMetaInformation]]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.ImageMetaInformation](0)))
    when(imageIndexService.indexDocument(any[domain.ImageMetaInformation]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.ImageMetaInformation](0)))
    when(tagIndexService.indexDocument(any[domain.ImageMetaInformation]))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.ImageMetaInformation](0)))

    writeService.deleteImageLanguageVersion(imageId, "nn")

    verify(imageRepository, times(1)).update(expectedImage, imageId)
  }

  test("That deleting last language version deletes entire image") {
    reset(imageRepository)
    reset(imageStorage)
    reset(imageIndexService)
    reset(tagIndexService)

    val imageId = 6666.toLong
    val image = multiLangImage.copy(
      id = Some(imageId),
      titles = List(domain.ImageTitle("english", "en")),
      captions = List(domain.ImageCaption("english", "en")),
      tags = Seq(domain.ImageTag(Seq("eng", "elsk"), "en")),
      alttexts = Seq(domain.ImageAltText("english", "en"))
    )

    when(imageRepository.withId(imageId)).thenReturn(Some(image))
    when(imageRepository.delete(eqTo(imageId))(any[DBSession])).thenReturn(1)
    when(imageStorage.deleteObject(any[String])).thenReturn(Success(()))
    when(imageIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Long](0)))
    when(tagIndexService.deleteDocument(any[Long])).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Long](0)))

    writeService.deleteImageLanguageVersion(imageId, "en")

    verify(imageStorage, times(1)).deleteObject(image.imageUrl)
    verify(imageIndexService, times(1)).deleteDocument(imageId)
    verify(tagIndexService, times(1)).deleteDocument(imageId)
    verify(imageRepository, times(1)).delete(eqTo(imageId))(any[DBSession])
  }
}
