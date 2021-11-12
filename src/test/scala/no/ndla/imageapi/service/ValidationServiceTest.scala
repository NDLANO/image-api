/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.mapping.License.CC_BY
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatra.servlet.FileItem
import org.mockito.Mockito._

import scala.util.Failure

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService = new ValidationService

  val fileMock = mock[FileItem]
  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val sampleImageMeta = ImageMetaInformation(
    Some(1),
    Seq.empty,
    Seq.empty,
    "image.jpg",
    1024,
    "image/jpeg",
    Copyright(CC_BY.toString, "", Seq.empty, Seq.empty, Seq.empty, None, None, None),
    Seq.empty,
    Seq.empty,
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
    reset(fileMock)
  }

  test("validateImageFile returns a validation message if file has an unknown extension") {
    val fileName = "image.asdf"
    when(fileMock.name).thenReturn(fileName)
    val Some(result) = validationService.validateImageFile(fileMock)

    result.message.contains(s"The file $fileName does not have a known file extension") should be(true)
  }

  test("validateImageFile returns a validation message if content type is unknown") {
    val fileName = "image.jpg"
    when(fileMock.name).thenReturn(fileName)
    when(fileMock.contentType).thenReturn(Some("text/html"))
    val Some(result) = validationService.validateImageFile(fileMock)

    result.message.contains(s"The file $fileName is not a valid image file.") should be(true)
  }

  test("validateImageFile returns None if image file is valid") {
    val fileName = "image.jpg"
    when(fileMock.name).thenReturn(fileName)
    when(fileMock.contentType).thenReturn(Some("image/jpeg"))
    validationService.validateImageFile(fileMock).isDefined should be(false)
  }

  test("validate returns a validation error if title contains html") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("<h1>title</h1>", "nb")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)
    exception.errors.head.message.contains("contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if title language is invalid") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("title", "invalid")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if title is valid") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("title", "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns a validation error if copyright contains an invalid license") {
    val imageMeta =
      sampleImageMeta.copy(copyright = Copyright("invalid", "", Seq.empty, Seq.empty, Seq.empty, None, None, None))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("invalid is not a valid license") should be(true)
  }

  test("validate returns a validation error if copyright origin contains html") {
    val imageMeta = sampleImageMeta.copy(
      copyright = Copyright(CC_BY.toString, "<h1>origin</h1>", Seq.empty, Seq.empty, Seq.empty, None, None, None))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if author contains html") {
    val imageMeta = sampleImageMeta.copy(
      copyright = Copyright(CC_BY.toString,
                            "",
                            Seq(Author("originator", "<h1>Drumpf</h1>")),
                            Seq.empty,
                            Seq.empty,
                            None,
                            None,
                            None))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns success if copyright is valid") {
    val imageMeta = sampleImageMeta.copy(
      copyright =
        Copyright(CC_BY.toString, "ntb", Seq(Author("originator", "Drumpf")), Seq.empty, Seq.empty, None, None, None))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns error if authortype is invalid") {
    val imageMeta = sampleImageMeta.copy(
      copyright =
        Copyright(CC_BY.toString, "ntb", Seq(Author("invalidType", "Drumpf")), Seq.empty, Seq.empty, None, None, None))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains(
      "Author is of illegal type. Must be one of originator, photographer, artist, writer, scriptwriter, reader, translator, director, illustrator, cowriter, composer") should be(
      true)
  }

  test("validate returns a validation error if tags contain html") {
    val imageMeta = sampleImageMeta.copy(tags = Seq(ImageTag(Seq("<h1>tag</h1>"), "en")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if tags language is invalid") {
    val imageMeta = sampleImageMeta.copy(tags = Seq(ImageTag(Seq("tag"), "invalid")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if tags are valid") {
    val imageMeta = sampleImageMeta.copy(tags = Seq(ImageTag(Seq("tag"), "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns a validation error if alt texts contain html") {
    val imageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("<h1>alt text</h1>", "en")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if alt texts language is invalid") {
    val imageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("alt text", "invalid")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if alt texts are valid") {
    val imageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("alt text", "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns a validation error if captions contain html") {
    val imageMeta = sampleImageMeta.copy(captions = Seq(ImageCaption("<h1>caption</h1>", "en")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be(true)
  }

  test("validate returns a validation error if captions language is invalid") {
    val imageMeta = sampleImageMeta.copy(captions = Seq(ImageCaption("caption", "invalid")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be(true)
  }

  test("validate returns success if captions are valid") {
    val imageMeta = sampleImageMeta.copy(captions = Seq(ImageCaption("caption", "en")))
    validationService.validate(imageMeta, None).isSuccess should be(true)
  }

  test("validate returns success if agreement exists") {
    when(draftApiClient.agreementExists(1)).thenReturn(true)
    val imageMeta = sampleImageMeta.copy(copyright = sampleImageMeta.copyright.copy(agreementId = Some(1)))
    val result = validationService.validate(imageMeta, None)
    result.isSuccess should be(true)
  }

  test("validate returns failure if agreement doesnt exist") {
    when(draftApiClient.agreementExists(1)).thenReturn(false)
    val imageMeta = sampleImageMeta.copy(copyright = sampleImageMeta.copyright.copy(agreementId = Some(1)))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)
    exception.errors.head.message.contains("Agreement with id 1 does not exist") should be(true)
    result.isSuccess should be(false)
  }

  test("validate returns error with invalid language if it does not already exist") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("new image title", "xyz")))
    val result = validationService.validate(imageMeta, None)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be(1)
    exception.errors.head.message.contains("Language 'xyz' is not a supported value.") should be(true)
    result.isSuccess should be(false)
  }

  test("validate returns success with unknown language if it already exist") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("new image title", "und")))
    val oldImageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("old image title", "und")))
    val result = validationService.validate(imageMeta, Some(oldImageMeta))
    result.isSuccess should be(true)
  }

  test("validate returns success with unknown language if it already exist, also in another field") {
    val imageMeta = sampleImageMeta.copy(titles = Seq(ImageTitle("new image title", "und")))
    val oldImageMeta = sampleImageMeta.copy(alttexts = Seq(ImageAltText("new image alttext", "und")))
    val result = validationService.validate(imageMeta, Some(oldImageMeta))
    result.isSuccess should be(true)
  }

}
