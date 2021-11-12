/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties.DefaultLanguage
import no.ndla.imageapi.auth.User
import no.ndla.imageapi.model.api.{ImageMetaInformationV2, NewImageMetaInformationV2, UpdateImageMetaInformation}
import no.ndla.imageapi.model.domain.{Image, ImageMetaInformation, LanguageField, ModelReleasedStatus}
import no.ndla.imageapi.model._
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageIndexService, TagIndexService}
import org.scalatra.servlet.FileItem

import java.io.ByteArrayInputStream
import java.lang.Math.max
import java.util.Date
import javax.imageio.ImageIO
import scala.util.{Failure, Random, Success, Try}

trait WriteService {
  this: ConverterService
    with ValidationService
    with ImageRepository
    with ImageIndexService
    with ImageStorageService
    with TagIndexService
    with Clock
    with User =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def deleteImageLanguageVersion(imageId: Long, language: String): Try[Option[ImageMetaInformationV2]] =
      imageRepository.withId(imageId) match {
        case Some(existing) if converterService.getSupportedLanguages(existing).contains(language) =>
          val newImage = converterService.withoutLanguage(existing, language)

          // If last language version delete entire image
          if (converterService.getSupportedLanguages(newImage).isEmpty)
            deleteImageAndFiles(imageId).map(_ => None)
          else
            updateImage(imageId, newImage, Some(existing), None).map(Some(_))

        case Some(_) =>
          Failure(new ImageNotFoundException(s"Image with id $imageId does not exist in language '$language'."))
        case None =>
          Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
      }

    def deleteImageAndFiles(imageId: Long) = {
      imageRepository.withId(imageId) match {
        case Some(toDelete) =>
          val metaDeleted = imageRepository.delete(imageId)
          val fileDeleted = imageStorage.deleteObject(toDelete.imageUrl)
          val indexDeleted = imageIndexService.deleteDocument(imageId).flatMap(tagIndexService.deleteDocument)

          if (metaDeleted < 1) {
            Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
          } else if (fileDeleted.isFailure) {
            Failure(new ImageStorageException("Something went wrong when deleting image file from storage."))
          } else {
            indexDeleted match {
              case Success(deleteId) => Success(deleteId)
              case Failure(ex)       => Failure(ex)
            }
          }
        case None =>
          Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
      }
    }

    def storeNewImage(newImage: NewImageMetaInformationV2, file: FileItem): Try[ImageMetaInformation] = {
      validationService.validateImageFile(file) match {
        case Some(validationMessage) => return Failure(new ValidationException(errors = Seq(validationMessage)))
        case _                       =>
      }

      val domainImage = uploadImage(file).flatMap(uploadedImage =>
        converterService.asDomainImageMetaInformationV2(newImage, uploadedImage)) match {
        case Failure(e)     => return Failure(e)
        case Success(image) => image
      }

      validationService.validate(domainImage, None) match {
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

      imageIndexService.indexDocument(imageMeta) match {
        case Success(_) =>
        case Failure(e) =>
          imageStorage.deleteObject(domainImage.imageUrl)
          imageRepository.delete(imageMeta.id.get)
          return Failure(e)
      }

      tagIndexService.indexDocument(imageMeta) match {
        case Success(_) => Success(imageMeta)
        case Failure(e) =>
          imageStorage.deleteObject(domainImage.imageUrl)
          imageIndexService.deleteDocument(imageMeta.id.get)
          tagIndexService.deleteDocument(imageMeta.id.get)
          imageRepository.delete(imageMeta.id.get)
          return Failure(e)
      }
    }

    private def hasChangedMetadata(lhs: domain.ImageMetaInformation, rhs: domain.ImageMetaInformation): Boolean = {
      val withoutMetas = (i: domain.ImageMetaInformation) =>
        i.copy(
          updated = new Date(0),
          updatedBy = ""
      )

      withoutMetas(lhs) != withoutMetas(rhs)
    }

    private[service] def mergeImages(existing: ImageMetaInformation,
                                     toMerge: UpdateImageMetaInformation): domain.ImageMetaInformation = {
      val now = clock.now()
      val userId = authUser.userOrClientid()

      val newImageMeta = existing.copy(
        titles = mergeLanguageFields(existing.titles,
                                     toMerge.title.toSeq.map(t => converterService.asDomainTitle(t, toMerge.language))),
        alttexts =
          mergeLanguageFields(existing.alttexts,
                              toMerge.alttext.toSeq.map(a => converterService.asDomainAltText(a, toMerge.language))),
        copyright = toMerge.copyright.map(c => converterService.toDomainCopyright(c)).getOrElse(existing.copyright),
        tags = mergeTags(existing.tags, toMerge.tags.toSeq.map(t => converterService.toDomainTag(t, toMerge.language))),
        captions =
          mergeLanguageFields(existing.captions,
                              toMerge.caption.toSeq.map(c => converterService.toDomainCaption(c, toMerge.language))),
        updated = now,
        updatedBy = userId,
        modelReleased = toMerge.modelReleased.flatMap(ModelReleasedStatus.valueOf).getOrElse(existing.modelReleased),
      )

      val existingLanguages = converterService.getSupportedLanguages(existing)
      val isNewLanguage = !existingLanguages.contains(toMerge.language)
      val newEditorNotes = {
        if (isNewLanguage)
          existing.editorNotes :+ domain.EditorNote(now, userId, s"Added new language '${toMerge.language}'.")
        else if (hasChangedMetadata(existing, newImageMeta))
          existing.editorNotes :+ domain.EditorNote(now, userId, "Updated image data.")
        else existing.editorNotes
      }

      newImageMeta.copy(editorNotes = newEditorNotes)
    }

    private def mergeTags(existing: Seq[domain.ImageTag], updated: Seq[domain.ImageTag]): Seq[domain.ImageTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    private def updateImage(imageId: Long,
                            image: domain.ImageMetaInformation,
                            oldImage: Option[domain.ImageMetaInformation],
                            language: Option[String]) = {
      for {
        validated <- validationService.validate(image, oldImage)
        updated = imageRepository.update(validated, imageId)
        indexed <- imageIndexService.indexDocument(updated)
        indexedByTags <- tagIndexService.indexDocument(indexed)
      } yield
        converterService.asApiImageMetaInformationWithDomainUrlV2(
          indexedByTags,
          Some(language.getOrElse(DefaultLanguage))
        )
    }

    private def overwriteImage(newFile: FileItem, oldImage: ImageMetaInformation): Try[ImageMetaInformation] = {
      for {
        uploaded <- uploadImage(newFile)
        _ <- imageStorage.cloneObject(uploaded.fileName, oldImage.imageUrl) // Overwrite old image with new one to make sure direct-urls are updated
      } yield converterService.withNewImage(oldImage, uploaded)
    }

    def updateImage(imageId: Long,
                    updateMeta: UpdateImageMetaInformation,
                    newFile: Option[FileItem]): Try[ImageMetaInformationV2] = {
      imageRepository.withId(imageId) match {
        case None => Failure(new ImageNotFoundException(s"Image with id $imageId found"))
        case Some(oldImage) =>
          val maybeOverwrittenImage = newFile match {
            case Some(file) =>
              validationService.validateImageFile(file) match {
                case Some(validationMessage) => Failure(new ValidationException(errors = Seq(validationMessage)))
                case _                       => overwriteImage(file, oldImage)
              }
            case _ => Success(oldImage)
          }

          maybeOverwrittenImage.flatMap(moi => {
            val newImage = mergeImages(moi, updateMeta)
            updateImage(
              imageId,
              newImage,
              Some(oldImage),
              Some(updateMeta.language)
            )
          })
      }
    }

    private[service] def mergeLanguageFields[A <: LanguageField[String]](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    private[service] def getFileExtension(fileName: String): Option[String] = {
      fileName.lastIndexOf(".") match {
        case index: Int if index > -1 => Some(fileName.substring(index))
        case _                        => None
      }
    }

    private def uploadImageWithName(file: FileItem, fileName: String): Try[Image] = {
      val contentType = file.getContentType.getOrElse("")
      val image = ImageIO.read(file.getInputStream)
      imageStorage
        .uploadFromStream(new ByteArrayInputStream(file.get()), fileName, contentType, file.size)
        .map(filePath => {
          Image(filePath, file.size, contentType, image.getWidth, image.getHeight)
        })
    }

    private[service] def uploadImage(file: FileItem): Try[Image] = {
      val extension = getFileExtension(file.name).getOrElse("")
      val fileName = LazyList.continually(randomFileName(extension)).dropWhile(imageStorage.objectExists).head
      uploadImageWithName(file, fileName)
    }

    private[service] def randomFileName(extension: String, length: Int = 12): String = {
      val extensionWithDot = if (extension.head == '.') extension else s".$extension"
      val randomString = Random.alphanumeric.take(max(length - extensionWithDot.length, 1)).mkString
      s"$randomString$extensionWithDot"
    }

  }

}
