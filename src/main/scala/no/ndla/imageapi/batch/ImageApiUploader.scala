/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.batch

import java.io._
import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model._
import no.ndla.imageapi.integration.AmazonIntegration

import scala.io.Source


object ImageApiUploader extends LazyLogging {

  def main(args: Array[String]) {

    logger.info("Antall argumenter: " + args.length)
    logger.info("Argumenter: " + args)

    if(args.length != 2){
      logger.info("Two arguments required: <path to input files> <range to run>")
      System.exit(1)
    }

    val path = args(0)
    val rangeFrom = args(1).split("-")(0).toInt
    val rangeTo = args(1).split("-")(1).toInt


    val imageMetaFile = path + "imagemeta.csv"
    val licensesFile = path + "license.csv"
    val authorsFile = path + "authors.csv"
    val originFile = path + "origin.csv"
    val failedFile = path + "failedImports.csv"

    logger.info(s"Running from $rangeFrom to $rangeTo")
    logger.info(s"ImageMeta: $imageMetaFile")
    logger.info(s"Licenses: $licensesFile")
    logger.info(s"Authors: $authorsFile")
    logger.info(s"Origin: $originFile")
    logger.info(s"Failed: $failedFile")
    logger.info("")

    new ImageApiUploader(
      maxUploads = rangeTo - rangeFrom,
      drop = rangeFrom,
      imageMetaFile = imageMetaFile,
      licensesFile = licensesFile,
      authorsFile = authorsFile,
      originFile = originFile,
      outputFile = failedFile)
      .uploadImages()
  }

}

/**
 *
 * @param maxUploads Max antall opplastinger som skal gjøres i kjøringen.
 *                   
 * @param imageMetaFile Fil som inneholder metainformasjon for bilder.
 *                      Kan produseres av følgende sql:
 *                      select n.nid, n.tnid, NULLIF(n.language,''), n.title,
 *                      NULLIF(cti.field_alt_text_value, ''),
 *                      from_unixtime(n.changed),
 *                      REPLACE(f.filepath, 'sites/default/files/images/', '') as original,
 *                      f.filemime as original_mime,
 *                      f.filesize as original_size
 *                      from node n
 *                      left join image i on (n.nid = i.nid)
 *                      left join files f on (i.fid = f.fid)
 *                      left join content_type_image cti on (n.nid = cti.nid)
 *                      where n.type = "image" and n.status = 1
 *                      and i.image_size = "_original"
 *                      order by n.nid desc limit 40000;
 *
 * @param licensesFile Fil som inneholder lisensinformasjon om bilder
 *                     Kan produseres med følgende sql:
 *                     SELECT n.nid, cc.license from node n
 *                       left join creativecommons_lite cc on (n.nid = cc.nid)
 *                     WHERE n.type = "image"
 *                       AND n.status = 1
 *                       AND NULLIF(cc.license,'') is not null;
 *
 * @param authorsFile Fil som inneholder authors for bilder.
 *                    Kan produseres av følgende sql:
 *                    SELECT n.nid AS image_nid, td.name AS author_type, person.title AS author FROM node n
 *                      LEFT JOIN ndla_authors na ON n.vid = na.vid
 *                      LEFT JOIN term_data td ON na.tid = td.tid
 *                      LEFT JOIN node person ON person.nid = na.person_nid
 *                    WHERE n.type = 'image' and n.status = 1
 *                      AND person.title is not null
 *                      AND td.name is not null
 *                    LIMIT 80000;
 *
 * @param originFile Fil som inneholder origins for bilder.
 *                   Kan produseres av følgende sql:
 *                   select n.nid, url.field_url_url from node n
 *                   left join content_field_url url ON url.vid = n.vid
 *                   where n.type = 'image' and n.status = 1 and url.field_url_url is not null
 *                   limit 40000;
 *
 *
 */
class ImageApiUploader(maxUploads:Int = 1, drop:Int = 0, imageMetaFile: String, licensesFile: String, authorsFile: String, originFile: String, outputFile: String) extends LazyLogging {

  val DownloadUrlPrefix = "http://ndla.no/sites/default/files/images/"
  val ThumbUrlPrefix = "http://ndla.no/sites/default/files/imagecache/fag_preset/images/"

  val languageToISOMap = Map(
    "nn" -> "nn",
    "nb" -> "nb",
    "en" -> "en"
  )

  val licenseToLicenseDefinitionsMap = Map(
    "by" -> License("by","Creative Commons Attribution 2.0 Generic", Option("https://creativecommons.org/licenses/by/2.0/")),
    "by-sa" -> License("by-sa","Creative Commons Attribution-ShareAlike 2.0 Generic", Option("https://creativecommons.org/licenses/by-sa/2.0/")),
    "by-nc" -> License("by-nc","Creative Commons Attribution-NonCommercial 2.0 Generic", Option("https://creativecommons.org/licenses/by-nc/2.0/")),
    "by-nd" -> License("by-nd","Creative Commons Attribution-NoDerivs 2.0 Generic", Option("https://creativecommons.org/licenses/by-nd/2.0/")),
    "by-nc-sa" -> License("by-nc-sa","Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Option("https://creativecommons.org/licenses/by-nc-sa/2.0/")),
    "by-nc-nd" -> License("by-nc-nd","Creative Commons Attribution-NonCommercial-NoDerivs 2.0 Generic", Option("https://creativecommons.org/licenses/by-nc-nd/2.0/")),
    "publicdomain" -> License("publicdomain","Public Domain", Option("https://creativecommons.org/about/pdm")),
    "gnu" -> License("gnu","GNU General Public License, version 2", Option("http://www.gnu.org/licenses/old-licenses/gpl-2.0.html")),
    "nolaw" -> License("nolaw","Public Domain Dedication", Option("http://opendatacommons.org/licenses/pddl/")),
    "nlod" -> License("nlod","Norsk lisens for offentlige data", Option("http://data.norge.no/nlod/no/1.0")),
    "noc" -> License("noc","Public Domain Mark", Option("https://creativecommons.org/about/pdm")),
    "copyrighted" -> License("copyrighted","Copyrighted", None)
  )

  val imageMeta = Source.fromFile(imageMetaFile)("UTF-8").getLines
    .map(line => toImageMeta(line))
    .toList

  val imageMetaMap = imageMeta
    .map(im => im.nid -> im)
    .toMap

  val translations = imageMeta
    .filter(_.tnid != "0")                                                     // Alle som har en tnid (translation-node-id)
    .filter(elem => elem.nid != elem.tnid)                                     // som ikke peker på seg selv
    .filter(elem => imageMetaMap.contains(elem.tnid))                          // hvor referansen eksisterer i datagrunnlaget
    .filter(elem => elem.originalFile == imageMetaMap(elem.tnid).originalFile) // hvor referert node har samme filsti til bilde
    .groupBy(_.tnid).map { case (k,v) => (k,v)}                                // med referert node som key i et map som peker på listen over oversettelser

  val imageMetaWithoutTranslations = imageMeta
    .filter(meta =>
      !translations.map(_._2)         // Få alle lister som er oversettelser av en annen node
      .flatten                        // og slå sammen til en liste
      .map(_.nid)                     // konverter til en liste av nid (node id)
      .toList.contains(meta.nid))     // ...og alle imageMeta som ikke er referet til derfra

  val imageLicense = Source.fromFile(licensesFile)("UTF-8").getLines
    .map(line => toImageLicense(line))
    .map(license => license.nid -> license)
    .toMap

  val imageOrigin = Source.fromFile(originFile)("UTF-8").getLines()
    .map(line => toImageOrigin(line))
    .map(origin => origin.nid -> origin)
    .toMap

  val imageAuthor = Source.fromFile(authorsFile)("UTF-8").getLines
    .map(line => toImageAuthor(line)).collect{case Some(x) => x}
    .map(author => author.nid -> author).toList
    .groupBy(_._1).map { case (k,v) => (k,v.map(_._2))}

  val imageStorage = AmazonIntegration.getImageStorageDefaultCredentials();
  val imageMetaStore = AmazonIntegration.getImageMeta()

  val failedMap = scala.collection.mutable.Map[String, String]()
  val retryMap = scala.collection.mutable.ListBuffer[ImageMeta]()

  var failed = 0
  var inserted = 0
  var updated = 0

  def uploadImages() = {
    val start = System.currentTimeMillis
    imageMetaWithoutTranslations.drop(drop).take(maxUploads).foreach(upload(_))

    logger.info(s"Inserted $inserted, updated $updated in ${System.currentTimeMillis - start}ms.")
    if(failed > 0){
      logger.info(s"$failed failed:")
      failedMap.foreach(elem => logger.info(s"${elem._1}:   ${elem._2}"))

      val writer = new PrintWriter(new File(outputFile))
      retryMap.foreach(meta => {
        writer.println(s"${meta.nid}#!#${meta.tnid}#!#${meta.language}#!#${meta.title}#!#${meta.alttext}#!#${meta.changed}#!#${meta.originalFile}#!#${meta.originalMime}#!#${meta.originalSize}")
      })
      writer.close()
    }
  }

  def upload(imageMeta: ImageMeta) = {
    val start = System.currentTimeMillis
    try {
      val tags = Tags.forImage(imageMeta.nid)

      val origin = imageOrigin.getOrElse(imageMeta.nid, ImageOrigin("", "")).origin
      val imageAuthors = imageAuthor.getOrElse(imageMeta.nid, List())
      val authors = imageAuthors.map(ia => Author(ia.typeAuthor, ia.name))
      val license = imageLicense.getOrElse(imageMeta.nid, ImageLicense("", "")).license
      val copyright = Copyright(licenseToLicenseDefinitionsMap.getOrElse(license, License(license, license, None)), origin, authors)

      var titles = List(ImageTitle(imageMeta.title, languageToISOMap.get(imageMeta.language)))
      var alttexts = List(ImageAltText(imageMeta.alttext, languageToISOMap.get(imageMeta.language)))
      translations.get(imageMeta.nid).foreach(_.foreach(translation => {
        titles = ImageTitle(translation.title, languageToISOMap.get(translation.language)) :: titles
        alttexts = ImageAltText(translation.alttext, languageToISOMap.get(translation.language)) :: alttexts
      }))

      imageMetaStore.withExternalId(imageMeta.nid) match {
        case Some(dbMeta) => {
          imageMetaStore.update(ImageMetaInformation(dbMeta.id, titles, alttexts, dbMeta.images, copyright, tags), dbMeta.id)
          updated += 1
          logger.info((inserted + updated + failed) + " - UPDATE: " + imageMeta.nid + " (" + imageMeta.title  + ") -- " + (System.currentTimeMillis - start) + "ms")
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

          if(!imageStorage.contains(thumbKey)) imageStorage.uploadFromByteArray(thumb, thumbKey, buffer)
          if(!imageStorage.contains(fullKey)) imageStorage.uploadFromUrl(full, fullKey, sourceUrlFull)

          imageMetaStore.insert(imageMetaInformation, imageMeta.nid)
          inserted += 1
          logger.info((inserted + updated + failed) + " - INSERT: " + imageMeta.nid + " (" + imageMeta.title  + ", " + sourceUrlFull + ") -- " + (System.currentTimeMillis - start) + "ms")
        }

      }
    } catch {
      case e: Exception  => {
        e.printStackTrace()
        failed += 1
        failedMap += imageMeta.nid -> s"${imageMeta.nid} failed after ${System.currentTimeMillis - start}ms with error: ${e.getMessage}"
        retryMap += imageMeta
      }
    }

  }

  def toImageMeta(line: String): ImageMeta = {
    val x = line.split("#!#") match {
      case Array(a, b, c, d, e, f, g, h, i) => (a, b, c, d, e, f, g, h, i)
    }
    (ImageMeta.apply _).tupled(x)
  }

  def toImageLicense(line: String): ImageLicense = {
    val x = line.split("#!#") match {
      case Array(a, b) => (a, b)
      case Array(a) => (a, "")
    }
    (ImageLicense.apply _).tupled(x)
  }

  def toImageOrigin(line: String): ImageOrigin = {
    val x = line.split("#!#") match {
      case Array(a, b) => (a, b)
      case Array(a) => (a, "")
    }
    (ImageOrigin.apply _).tupled(x)
  }

  def toImageAuthor(line: String): Option[ImageAuthor] = {
    line.split("#!#") match {
      case Array(a, b, c) => {
        if(c == "NULL") None else Option((ImageAuthor.apply _).tupled((a,replaceNULLWithEmtpy(b),c)))
      }
      case _ => None
    }
  }

  def replaceNULLWithEmtpy(str: String): String = {
    if(str == "NULL") "" else str
  }
}


case class ImageMeta(nid:String, tnid:String, language:String, title:String, alttext:String, changed:String, originalFile:String, originalMime: String, originalSize: String)
case class ImageLicense(nid:String, license:String)
case class ImageAuthor(nid: String, typeAuthor:String, name:String)
case class ImageOrigin(nid: String, origin:String)