package no.ndla.imageapi.batch.service

import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.batch._
import no.ndla.imageapi.batch.integration.CMDataComponent
import no.ndla.imageapi.model.{Image, ImageMetaInformation, ImageVariants, _}
import no.ndla.imageapi.repository.ImageRepositoryComponent
import no.ndla.imageapi.service.AmazonImageStorageComponent

trait ImportServiceComponent {
  this: CMDataComponent with AmazonImageStorageComponent with ImageRepositoryComponent =>
  val importService: ImportService

  class ImportService extends LazyLogging {
    val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"
    val ThumbUrlPrefix = "http://ndla.no/sites/default/files/imagecache/fag_preset/images/"
    val licenseToLicenseDefinitionsMap = Map(
      "by" -> License("by", "Creative Commons Attribution 2.0 Generic", Option("https://creativecommons.org/licenses/by/2.0/")),
      "by-sa" -> License("by-sa", "Creative Commons Attribution-ShareAlike 2.0 Generic", Option("https://creativecommons.org/licenses/by-sa/2.0/")),
      "by-nc" -> License("by-nc", "Creative Commons Attribution-NonCommercial 2.0 Generic", Option("https://creativecommons.org/licenses/by-nc/2.0/")),
      "by-nd" -> License("by-nd", "Creative Commons Attribution-NoDerivs 2.0 Generic", Option("https://creativecommons.org/licenses/by-nd/2.0/")),
      "by-nc-sa" -> License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Option("https://creativecommons.org/licenses/by-nc-sa/2.0/")),
      "by-nc-nd" -> License("by-nc-nd", "Creative Commons Attribution-NonCommercial-NoDerivs 2.0 Generic", Option("https://creativecommons.org/licenses/by-nc-nd/2.0/")),
      "publicdomain" -> License("publicdomain", "Public Domain", Option("https://creativecommons.org/about/pdm")),
      "gnu" -> License("gnu", "GNU General Public License, version 2", Option("http://www.gnu.org/licenses/old-licenses/gpl-2.0.html")),
      "nolaw" -> License("nolaw", "Public Domain Dedication", Option("http://opendatacommons.org/licenses/pddl/")),
      "nlod" -> License("nlod", "Norsk lisens for offentlige data", Option("http://data.norge.no/nlod/no/1.0")),
      "noc" -> License("noc", "Public Domain Mark", Option("https://creativecommons.org/about/pdm")),
      "copyrighted" -> License("copyrighted", "Copyrighted", None)
    )
    val languageToISOMap = Map(
      "nn" -> "nn",
      "nb" -> "nb",
      "en" -> "en"
    )

    // Import nodes in range rangeFrom to rangeTo.
    // Return a tuple (num. images imported, num. images failed to import)
    def doImport(rangeFrom: Int, rangeTo: Int): (Int, Int) = {
      val imageMeta = cmData.imageMetas(1)
      val imageAuthor = cmData.imageAuthors(10)
        .map(author => author.nid -> author)
        .groupBy(_._1).map { case (k,v) => (k,v.map(_._2))}
      val imageLicense = cmData.imageLicences
        .map(license => license.nid -> license)
        .toMap
      val imageOrigin = cmData.imageOrigins(1).map(origin => origin.nid -> origin).toMap

      val imageMetaMap = imageMeta
        .map(im => im.nid -> im)
        .toMap

      val translations = imageMeta
        .filter(_.tnid != "0") // Alle som har en tnid (translation-node-id)
        .filter(elem => elem.nid != elem.tnid) // som ikke peker på seg selv
        .filter(elem => imageMetaMap.contains(elem.tnid)) // hvor referansen eksisterer i datagrunnlaget
        .filter(elem => elem.originalFile == imageMetaMap(elem.tnid).originalFile) // hvor referert node har samme filsti til bilde
        .groupBy(_.tnid).map { case (k, v) => (k, v) } // med referert node som key i et map som peker på listen over oversettelser

      val imageMetaWithoutTranslations = imageMeta
        .filter(meta =>
          !translations.map(_._2) // Få alle lister som er oversettelser av en annen node
            .flatten // og slå sammen til en liste
            .map(_.nid) // konverter til en liste av nid (node id)
            .toList.contains(meta.nid)) // ...og alle imageMeta som ikke er referet til derfra

      imageMetaWithoutTranslations.drop(rangeFrom).take(rangeTo - rangeFrom).foldLeft((0, 0)) { (result, current) => {
        val origin = imageOrigin.getOrElse(current.nid, ImageOrigin("", ""))
        val author = imageAuthor.getOrElse(current.nid, List())
        val license = imageLicense.getOrElse(current.nid, ImageLicense("", ""))

        upload(current, origin, author, license, translations) match {
          case Some(imageMeta) => (result._1, result._2 + 1) // Fail
          case _ => (result._1 + 1, result._2) // Success
        }
      }}
    }

    def upload(imageMeta: ImageMeta, origin: ImageOrigin, imageAuthors: List[ImageAuthor],
               license: ImageLicense, translations: Map[String, List[ImageMeta]]): Option[ImageMeta] = {
      val start = System.currentTimeMillis
      try {
        val tags = Tags.forImage(imageMeta.nid)

        val authors = imageAuthors.map(ia => Author(ia.typeAuthor, ia.name))
        val copyright = Copyright(licenseToLicenseDefinitionsMap.getOrElse(license.license, License(license.license, license.license, None)), origin.origin, authors)

        var titles = List(ImageTitle(imageMeta.title, languageToISOMap.get(imageMeta.language)))
        var alttexts = List(ImageAltText(imageMeta.alttext, languageToISOMap.get(imageMeta.language)))
        translations.get(imageMeta.nid).foreach(_.foreach(translation => {
          titles = ImageTitle(translation.title, languageToISOMap.get(translation.language)) :: titles
          alttexts = ImageAltText(translation.alttext, languageToISOMap.get(translation.language)) :: alttexts
        }))

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

            if (!amazonImageStorage.contains(thumbKey)) amazonImageStorage.uploadFromByteArray(thumb, thumbKey, buffer)
            if (!amazonImageStorage.contains(fullKey)) amazonImageStorage.uploadFromUrl(full, fullKey, sourceUrlFull)

            imageRepository.insert(imageMetaInformation, imageMeta.nid)
            logger.info(s"inserted {} ({}, {}) -- ${(System.currentTimeMillis - start)} ms", imageMeta.nid, imageMeta.title, sourceUrlFull)
          }
        }
        None
      } catch {
        case e: Exception => {
          e.printStackTrace()
          logger.info(s"${imageMeta.nid} failed after ${System.currentTimeMillis - start}ms with error: ${e.getMessage}")
          Some(imageMeta)
        }
      }
    }
  }
}
