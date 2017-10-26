package no.ndla.imageapi.service

import java.io.ByteArrayInputStream
import java.lang.Math.max

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model.{ImageNotFoundException, ValidationException}
import no.ndla.imageapi.model.api.{ImageMetaInformationV2, NewImageMetaInformationV2, UpdateImageMetaInformation}
import no.ndla.imageapi.model.domain.{Image, ImageMetaInformation, LanguageField}
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexService
import org.scalatra.servlet.FileItem

import scala.util.{Failure, Random, Success, Try}

trait WriteService {
  this: ConverterService with ValidationService with ImageRepository with IndexService with ImageStorageService with Clock with User =>
  val writeService: WriteService

  class WriteService extends LazyLogging {
    def storeNewImage(newImage: NewImageMetaInformationV2, file: FileItem): Try[ImageMetaInformation] = {
      validationService.validateImageFile(file) match {
        case Some(validationMessage) => return Failure(new ValidationException(errors=Seq(validationMessage)))
        case _ =>
      }

      val domainImage = uploadImage(file).map(uploadedImage =>
          converterService.asDomainImageMetaInformationV2(newImage, uploadedImage)) match {
            case Failure(e) => return Failure(e)
            case Success(image) => image
      }

      validationService.validate(domainImage) match {
        case Failure(e) =>
          imageStorage.deleteObject(domainImage.imageUrl)
          return Failure(e)
        case _ =>
      }

      val imageMeta = Try(imageRepository.insert(domainImage)) match {
        case Success(meta) => meta
        case Failure(e) =>
          imageStorage.deleteObject(domainImage.imageUrl)
          return Failure(e)
      }

      indexService.indexDocument(imageMeta) match {
        case Success(_) => Success(imageMeta)
        case Failure(e) =>
          imageStorage.deleteObject(domainImage.imageUrl)
          imageRepository.delete(imageMeta.id.get)
          Failure(e)
      }
    }

    private def mergeImages(existing: ImageMetaInformation, toMerge: UpdateImageMetaInformation): domain.ImageMetaInformation = {
      val now = clock.now()
      val userId = authUser.id()

      existing.copy(
        titles = mergeLanguageFields(existing.titles, toMerge.title.toSeq.map(t => converterService.asDomainTitle(t, toMerge.language))),
        alttexts = mergeLanguageFields(existing.alttexts, toMerge.alttext.toSeq.map(a => converterService.asDomainAltText(a, toMerge.language))),
        copyright = toMerge.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
        tags = mergeTags(existing.tags, toMerge.tags.toSeq.map(t => converterService.toDomainTag(t, toMerge.language))),
        captions = mergeLanguageFields(existing.captions, toMerge.caption.toSeq.map(c => converterService.toDomainCaption(c, toMerge.language))),
        updated = now,
        updatedBy = userId
      )
    }

    private def mergeTags(existing: Seq[domain.ImageTag], updated: Seq[domain.ImageTag]): Seq[domain.ImageTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    def updateImage(imageId: Long, image: UpdateImageMetaInformation): Try[ImageMetaInformationV2] = {
      val updateImage = imageRepository.withId(imageId) match {
        case None => Failure(new ImageNotFoundException(s"Image with id $imageId found"))
        case Some(existing) => Success(mergeImages(existing, image))
      }

      updateImage.flatMap(validationService.validate)
        .map(imageMeta => imageRepository.update(imageMeta, imageId))
        .flatMap(indexService.indexDocument)
        .map(updatedImage => converterService.asApiImageMetaInformationWithDomainUrlV2(updatedImage, Some(image.language)).get)
    }

    private[service] def mergeLanguageFields[A <: LanguageField[String]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    private[service] def getFileExtension(fileName: String): Option[String] = {
      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 => Some(fileName.substring(index))
        case _ => None
      }
    }

    private[service] def uploadImage(file: FileItem): Try[Image] = {
      val extension = getFileExtension(file.name).getOrElse("")
      val contentType = file.getContentType.getOrElse("")
      val fileName = Stream.continually(randomFileName(extension)).dropWhile(imageStorage.objectExists).head

      imageStorage.uploadFromStream(new ByteArrayInputStream(file.get), fileName, contentType, file.size).map(filePath => {
        Image(filePath, file.size, contentType)
      })
    }

    private[service] def randomFileName(extension: String, length: Int = 12): String = {
      val extensionWithDot = if (extension.head == '.') extension else s".$extension"
      val randomString = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

  }
}
