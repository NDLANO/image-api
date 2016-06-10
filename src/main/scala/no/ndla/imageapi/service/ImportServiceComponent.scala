package no.ndla.imageapi.service

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration.CMDataComponent
import no.ndla.imageapi.model.{Image, ImageMetaInformation, ImageVariants, _}
import no.ndla.imageapi.repository.ImageRepositoryComponent
import no.ndla.mapping.{ISO639Mapping, LicenseMapping}

trait ImportServiceComponent {
  this: CMDataComponent with ImageStorageService with ImageRepositoryComponent =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"
    val ThumbUrlPrefix = "http://ndla.no/sites/default/files/imagecache/fag_preset/images/"

    def importImage(imageId: String): Option[String] = {
      val meta = cmData.imageMeta(imageId)
      meta match {
        case Some(img) => if (img.isTranslation) return importImage(img.tnid)
        case None => throw new ImageNotFoundException(s"Image with id $imageId was not found")
      }

      val author = cmData.imageAuthor(imageId)
      val license = cmData.imageLicence(imageId).getOrElse(ImageLicense(meta.get.nid, meta.get.tnid, ""))
      val origin = cmData.imageOrigin(imageId).getOrElse(ImageOrigin(meta.get.nid, meta.get.tnid, ""))

      val translations = cmData.imageMetaTranslations(meta.get.tnid)
        .filter(elem => elem.originalFile == meta.get.originalFile) // hvor referert node har samme filsti til bilde

      upload(meta.get, origin, author, license, translations) match {
        case Some(errorMsg) => Some(errorMsg) // Fail
        case _ => None // Success
      }
    }

    def upload(imageMeta: ImageMeta, origin: ImageOrigin, imageAuthors: List[ImageAuthor],
               license: ImageLicense, translations: List[ImageMeta]): Option[String] = {
      val start = System.currentTimeMillis
      try {
        val tags = Tags.forImage(imageMeta.nid)

        val authors = imageAuthors.map(ia => Author(ia.typeAuthor, ia.name))
        val _license = LicenseMapping.getLicenseDefinition(license.license) match {
          case Some((description, url)) => License(license.license, description, Some(url))
          case None => License(license.license, license.license, None)
        }
        val copyright = Copyright(_license, origin.origin, authors)

        val imageLang = ISO639Mapping.languageCodeSupported(imageMeta.language) match {
          case true => Some(imageMeta.language)
          case false => None
        }
        val translationLang = ISO639Mapping.languageCodeSupported(imageMeta.language) match {
          case true => Some(imageMeta.language)
          case false => None
        }
        var titles = List(ImageTitle(imageMeta.title, imageLang))
        var alttexts = List(ImageAltText(imageMeta.alttext, imageLang))
        translations.foreach(translation => {
          titles = ImageTitle(translation.title, translationLang) :: titles
          alttexts = ImageAltText(translation.alttext, translationLang) :: alttexts
        })

        imageRepository.withExternalId(imageMeta.nid) match {
          case Some(dbMeta) => {
            imageRepository.update(ImageMetaInformation(dbMeta.id, titles, alttexts, dbMeta.images, copyright, tags), dbMeta.id)
            logger.info(s"updated {} ({}) -- ${(System.currentTimeMillis - start)} ms", imageMeta.nid, imageMeta.title)
          }
          case None => {
            val sourceUrlFull = DownloadUrlPrefix + imageMeta.originalFile
            val sourceUrlThumb = ThumbUrlPrefix + imageMeta.originalFile

            val imageStream = new URL(sourceUrlThumb).openStream()
            val buffer = Stream.continually(imageStream.read).takeWhile(_ != -1).map(_.toByte).toArray

            val thumbKey = "thumbs/" + imageMeta.originalFile
            val thumb = Image(thumbKey, buffer.size, imageMeta.originalMime)

            val fullKey = "full/" + imageMeta.originalFile
            val full = Image(fullKey, imageMeta.originalSize.toInt, imageMeta.originalMime)

            val imageMetaInformation = ImageMetaInformation("0", titles, alttexts, ImageVariants(Option(thumb), Option(full)), copyright, tags)

            if (!imageStorage.contains(thumbKey)) imageStorage.uploadFromByteArray(thumb, thumbKey, buffer)
            if (!imageStorage.contains(fullKey)) imageStorage.uploadFromUrl(full, fullKey, sourceUrlFull)

            imageRepository.insert(imageMetaInformation, imageMeta.nid)
            logger.info(s"inserted {} ({}, {}) -- ${(System.currentTimeMillis - start)} ms", imageMeta.nid, imageMeta.title, sourceUrlFull)
          }
        }
        None
      } catch {
        case e: Exception => {
          e.printStackTrace()
          val errMsg = s"Import of node ${imageMeta.nid} failed after ${System.currentTimeMillis - start} ms with error: ${e.getMessage}"
          logger.info(errMsg)
          Some(errMsg)
        }
      }
    }
  }
}

case class ImageMeta(nid:String, tnid:String, language:String, title:String, alttext:String, changed:String, originalFile:String, originalMime: String, originalSize: String) {
  def isMainImage = nid == tnid || tnid == "0"
  def isTranslation = !isMainImage
}
case class ImageLicense(nid:String, tnid: String, license:String)
case class ImageAuthor(nid: String, tnid: String, typeAuthor:String, name:String)
case class ImageOrigin(nid: String, tnid: String, origin:String)
