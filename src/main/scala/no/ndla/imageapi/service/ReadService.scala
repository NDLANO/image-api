package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.dsl._
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model.api.ImageMetaInformationV2
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexService

import scala.util.Try

trait ReadService {
  this: ConverterService
    with ValidationService
    with ImageRepository
    with IndexService
    with ImageStorageService
    with Clock
    with User =>
  val readService: ReadService

  class ReadService extends LazyLogging {

    def withId(imageId: Long, language: Option[String]): Option[ImageMetaInformationV2] =
      imageRepository
        .withId(imageId)
        .flatMap(image => converterService.asApiImageMetaInformationWithApplicationUrlV2(image, language))

    def getIdFromIdPath(path: String): Option[Long] = { ??? }

    def getFilePathFromRawPath(path: String): Option[String] = { ??? }

    def getImageMetaFromPath(path: String): Try[ImageMetaInformationV2] = {

      val pathParts = path.path.parts
      val isIdPath = pathParts.contains("id")

      if (isIdPath) {
        getIdFromIdPath(path)
        ???
      } else {
        getFilePathFromRawPath(path).map(imageRepository.getImageFromFilePath)
        ???
      }

    }
  }

}
