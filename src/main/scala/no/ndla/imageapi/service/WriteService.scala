package no.ndla.imageapi.service

import java.io.ByteArrayInputStream
import java.lang.Math.max

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.api.{NewImageMetaInformation}
import no.ndla.imageapi.model.domain.{Image, ImageMetaInformation}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexService
import org.scalatra.servlet.FileItem

import scala.util.{Failure, Random, Success, Try}

trait WriteService {
  this: ConverterService with ValidationService with ImageRepository with IndexService with ImageStorageService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {
    def storeNewImage(newImage: NewImageMetaInformation, file: FileItem): Try[ImageMetaInformation] = {
      validationService.validateImageFile(file) match {
        case Some(validationMessage) => return Failure(new ValidationException(errors=Seq(validationMessage)))
        case _ =>
      }

      val domainImage = uploadImage(file).map(uploadedImage =>
          converterService.asDomainImageMetaInformation(newImage, uploadedImage)) match {
        case Failure(e) => return Failure(e)
        case Success(image) => image
      }

      validationService.validate(domainImage) match {
        case Failure(e) =>
          imageStorage.deleteObject(domainImage.imageUrl)
          return Failure(e)
        case _ =>
      }

      val imageMeta = Try(imageRepository.insert(domainImage, newImage.externalId)) match {
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
