package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.api.{ImageMetaInformation, NewImageMetaInformation}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.IndexService
import org.scalatra.servlet.FileItem

import scala.util.{Failure, Try}

trait WriteService {
  this: ConverterService with ValidationService with ImageRepository with IndexService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {
    def storeNewImage(newImage: NewImageMetaInformation, files: Seq[FileItem]): Try[ImageMetaInformation] = {
      Failure(new NotImplementedError("TODO"))
    }
  }
}
