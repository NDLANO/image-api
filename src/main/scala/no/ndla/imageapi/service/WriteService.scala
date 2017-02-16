package no.ndla.imageapi.service

import java.io.ByteArrayInputStream
import java.lang.Math.max

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.api.{ImageMetaInformation, NewImageMetaInformation}
import no.ndla.imageapi.model.domain.Image
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

      val uploadedImage = uploadImage(file) match {
        case Failure(e) => return Failure(e)
        case Success(image) => image
      }

      val result = for {
        imageMeta <- Try(converterService.asDomainImageMetaInformation(newImage, uploadedImage))
        _ <- validationService.validate(imageMeta)
        r <- Try(imageRepository.insert(imageMeta))
        _ <- indexService.indexDocument(r)
      } yield converterService.asApiImageMetaInformationWithApplicationUrl(r)

      if (result.isFailure)
        imageStorage.deleteObject(uploadedImage.fileName)

      result
    }

    def getFileExtension(fileName: String): Option[String] = {
      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 => Some(fileName.substring(index))
        case _ => None
      }
    }

    def uploadImage(file: FileItem): Try[Image] = {
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
