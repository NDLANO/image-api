package no.ndla.imageapi.service

import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import no.ndla.mapping.License.getLicense
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.scalatra.servlet.FileItem

import scala.util.{Failure, Success, Try}

trait ValidationService {
  val validationService: ValidationService

  class ValidationService {
    def validateImageFile(imageFile: FileItem): Option[ValidationMessage] = {
      val validMimeTypes = Seq("image/bmp", "image/gif", "image/jpeg", "image/x-citrix-jpeg", "image/pjpeg", "image/png", "image/x-citrix-png", "image/x-png", "image/svg+xml")
      val actualMimeType = imageFile.contentType.getOrElse("")

      if (!validMimeTypes.contains(actualMimeType))
        return Some(ValidationMessage("file", s"The file ${imageFile.name} is not a valid image file. Only valid type is '${validMimeTypes.mkString(",")}', but was '$actualMimeType'"))

      val validFileExtensions = Seq(".jpg", ".png", ".jpg", ".jpeg", ".bmp", ".gif", ".svg")
      if (imageFile.name.toLowerCase.endsWith(validFileExtensions))
        return None

      Some(ValidationMessage("file", s"The file ${imageFile.name} does not have a known file extension. Must be one of ${validFileExtensions.mkString(",")}"))
    }

    def validate(image: ImageMetaInformation): Try[ImageMetaInformation] = {
      val validationMessages = image.titles.flatMap(title => validateTitle("title", title))  ++
        validateCopyright(image.copyright) ++
        validateTags(image.tags)

      if (validationMessages.isEmpty)
        return Success(image)

      Failure(new ValidationException(errors = validationMessages))
    }

    private def validateTitle(fieldPath: String, title: ImageTitle): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, title.title).toList ++
        validateLanguage(fieldPath, title.language)
    }

    def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      validateLicense(copyright.license).toList ++
      copyright.authors.flatMap(validateAuthor) ++
      containsNoHtml("copyright.origin", copyright.origin)
    }

    def validateLicense(license: License): Seq[ValidationMessage] = {
      getLicense(license.license) match {
        case None => Seq(ValidationMessage("license.license", s"$license is not a valid license"))
        case _ => Seq()
      }
    }

    def validateAuthor(author: Author): Seq[ValidationMessage] = {
      containsNoHtml("author.type", author.`type`).toList ++
        containsNoHtml("author.name", author.name).toList
    }

    def validateTags(tags: Seq[ImageTag]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(containsNoHtml("tags.tags", _)).toList :::
          validateLanguage("tags.language", tagList.language).toList
      })
    }

    private def containsNoHtml(fieldPath: String, text: String): Option[ValidationMessage] = {
      Jsoup.isValid(text, Whitelist.none()) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed"))
      }
    }

    private def validateLanguage(fieldPath: String, languageCode: Option[String]): Option[ValidationMessage] = {
      languageCode.flatMap(lang =>
        languageCodeSupported6391(lang) match {
          case true => None
          case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
        })
    }

    private def languageCodeSupported6391(languageCode: String): Boolean =
      get6391CodeFor6392CodeMappings.exists(_._2 == languageCode)

  }
}
