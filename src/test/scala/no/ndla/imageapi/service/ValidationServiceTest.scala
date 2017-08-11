package no.ndla.imageapi.service

import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatra.servlet.FileItem
import org.mockito.Mockito._

import scala.util.Failure

class ValidationServiceTest extends UnitSuite with TestEnvironment {
  override val validationService = new ValidationService

  val fileMock = mock[FileItem]
  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val sampleImageMeta = ImageMetaInformation(Some(1), Seq.empty, Seq.empty, "image.jpg", 1024, "image/jpeg", Copyright(License("by", "by", None), "", Seq.empty), Seq.empty, Seq.empty, "ndla124", updated())

  override def beforeEach = {
    reset(fileMock)
  }

  test("validateImageFile returns a validation message if file has an unknown extension") {
    val fileName = "image.asdf"
    when(fileMock.name).thenReturn(fileName)
    val Some(result) = validationService.validateImageFile(fileMock)

    result.message.contains(s"The file $fileName does not have a known file extension") should be (true)
  }

  test("validateImageFile returns a validation message if content type is unknown") {
    val fileName = "image.jpg"
    when(fileMock.name).thenReturn(fileName)
    when(fileMock.contentType).thenReturn(Some("text/html"))
    val Some(result) = validationService.validateImageFile(fileMock)

    result.message.contains(s"The file $fileName is not a valid image file.") should be (true)
  }

  test("validateImageFile returns None if image file is valid") {
    val fileName = "image.jpg"
    when(fileMock.name).thenReturn(fileName)
    when(fileMock.contentType).thenReturn(Some("image/jpeg"))
    validationService.validateImageFile(fileMock).isDefined should be (false)
  }

  test("validate returns a validation error if title contains html") {
    val imageMeta = sampleImageMeta.copy(titles=Seq(ImageTitle("<h1>title</h1>", "nb")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)
    exception.errors.head.message.contains("contains illegal html-characters") should be (true)
  }

  test("validate returns a validation error if title language is invalid") {
    val imageMeta = sampleImageMeta.copy(titles=Seq(ImageTitle("title", "invalid")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be (true)
  }

  test("validate returns success if title is valid") {
    val imageMeta = sampleImageMeta.copy(titles=Seq(ImageTitle("title", "en")))
    validationService.validate(imageMeta).isSuccess should be (true)
  }

  test("validate returns a validation error if copyright contains an invalid license") {
    val imageMeta = sampleImageMeta.copy(copyright=Copyright(License("invalid", "", None), "", Seq.empty))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("invalid is not a valid license") should be (true)
  }

  test("validate returns a validation error if copyright origin contains html") {
    val imageMeta = sampleImageMeta.copy(copyright=Copyright(License("by", "", None), "<h1>origin</h1>", Seq.empty))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be (true)
  }

  test("validate returns a validation error if author contains html") {
    val imageMeta = sampleImageMeta.copy(copyright=Copyright(License("by", "", None), "", Seq(Author("author", "<h1>Drumpf</h1>"))))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be (true)
  }

  test("validate returns success if copyright is valid") {
    val imageMeta = sampleImageMeta.copy(copyright=Copyright(License("by", "", None), "ntb", Seq(Author("author", "Drumpf"))))
    validationService.validate(imageMeta).isSuccess should be (true)
  }

  test("validate returns a validation error if tags contain html") {
    val imageMeta = sampleImageMeta.copy(tags=Seq(ImageTag(Seq("<h1>tag</h1>"), "en")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be (true)
  }

  test("validate returns a validation error if tags language is invalid") {
    val imageMeta = sampleImageMeta.copy(tags=Seq(ImageTag(Seq("tag"), "invalid")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be (true)
  }

  test("validate returns success if tags are valid") {
    val imageMeta = sampleImageMeta.copy(tags=Seq(ImageTag(Seq("tag"), "en")))
    validationService.validate(imageMeta).isSuccess should be (true)
  }

  test("validate returns a validation error if alt texts contain html") {
    val imageMeta = sampleImageMeta.copy(alttexts=Seq(ImageAltText("<h1>alt text</h1>", "en")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be (true)
  }

  test("validate returns a validation error if alt texts language is invalid") {
    val imageMeta = sampleImageMeta.copy(alttexts=Seq(ImageAltText("alt text", "invalid")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be (true)
  }

  test("validate returns success if alt texts are valid") {
    val imageMeta = sampleImageMeta.copy(alttexts=Seq(ImageAltText("alt text", "en")))
    validationService.validate(imageMeta).isSuccess should be (true)
  }

  test("validate returns a validation error if captions contain html") {
    val imageMeta = sampleImageMeta.copy(captions=Seq(ImageCaption("<h1>caption</h1>", "en")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("The content contains illegal html-characters") should be (true)
  }

  test("validate returns a validation error if captions language is invalid") {
    val imageMeta = sampleImageMeta.copy(captions=Seq(ImageCaption("caption", "invalid")))
    val result = validationService.validate(imageMeta)
    val exception = result.failed.get.asInstanceOf[ValidationException]
    exception.errors.length should be (1)

    exception.errors.head.message.contains("Language 'invalid' is not a supported value") should be (true)
  }

  test("validate returns success if captions are valid") {
    val imageMeta = sampleImageMeta.copy(captions=Seq(ImageCaption("caption", "en")))
    validationService.validate(imageMeta).isSuccess should be (true)
  }

}
