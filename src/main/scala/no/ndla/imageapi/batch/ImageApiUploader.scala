/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.batch

import no.ndla.imageapi.model._
import no.ndla.imageapi.integration.AmazonIntegration

import scala.io.Source


object ImageApiUploader {

  def main(args: Array[String]) {



    new ImageApiUploader(maxUploads = 300,
      imageMetaFile = "/Users/kes/sandboxes/ndla/data-dump/20151006_0957/imagemeta.csv",
      licensesFile = "/Users/kes/sandboxes/ndla/data-dump/20151006_0957/license.csv",
      authorsFile = "/Users/kes/sandboxes/ndla/data-dump/20151006_0957/authors.csv",
      originFile = "/Users/kes/sandboxes/ndla/data-dump/20151006_0957/origin.csv")
      .uploadImages()
  }

}

/**
 *
 * @param maxUploads Max antall opplastinger som skal gjøres i kjøringen.
 *                   
 * @param imageMetaFile Fil som inneholder metainformasjon for bilder.
 *                      Kan produseres av følgende sql:
 *                       select n.nid, n.tnid, NULLIF(n.language,''), n.title,
 *                         NULLIF(cti.field_alt_text_value, ''),
 *                         from_unixtime(n.changed),
 *                         f.filepath as original,
 *                         f.filemime as original_mime,
 *                         f.filesize as original_size,
 *                         f2.filepath as thumb,
 *                         f2.filemime as thumb_mime,
 *                         f2.filesize as thumb_size
 *                       from node n
 *                         left join image i on (n.nid = i.nid)
 *                         left join image i2 on (n.nid = i2.nid)
 *                         left join files f on (i.fid = f.fid)
 *                         left join files f2 on (i2.fid = f2.fid)
 *                         left join content_type_image cti on (n.nid = cti.nid)
 *                       where n.type = "image" and n.status = 1
 *                         and i.image_size = "_original"
 *                         and i2.image_size = "thumbnail"
 *                       order by n.nid desc limit 400;
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
class ImageApiUploader(maxUploads:Int = 1, imageMetaFile: String, licensesFile: String, authorsFile: String, originFile: String) {

  val DownloadUrlPrefix = "http://cm.test.ndla.no/"

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

  val failed = scala.collection.mutable.Map[String, String]()
  var inserted = 0
  var updated = 0

  def uploadImages() = {
    val start = System.currentTimeMillis
    imageMetaWithoutTranslations.take(maxUploads).foreach(upload(_))

    println(s"Inserted $inserted, updated $updated in ${System.currentTimeMillis - start}ms.")
    if(!failed.isEmpty){
      println("The following failed:")
      failed.foreach(elem => println(s"${elem._1}:   ${elem._2}"))
    }
  }

  def upload(imageMeta: ImageMeta) = {
    val start = System.currentTimeMillis
    try {
      val license = imageLicense.getOrElse(imageMeta.nid, ImageLicense("", "")).license
      val origin = imageOrigin.getOrElse(imageMeta.nid, ImageOrigin("", "")).origin
      val imageAuthors = imageAuthor.getOrElse(imageMeta.nid, List())
      val sourceUrlFull = DownloadUrlPrefix + imageMeta.originalFile
      val sourceUrlThumb = DownloadUrlPrefix + imageMeta.thumbFile
      val tags = Tags.forImage(imageMeta.nid)

      val thumbKey = imageMeta.thumbFile.replace("sites/default/files/images/", "thumbs/")
      val thumb = Image(thumbKey, imageMeta.thumbSize.toInt, imageMeta.thumbMime)

      val fullKey = imageMeta.originalFile.replace("sites/default/files/images/", "full/")
      val full = Image(fullKey, imageMeta.originalSize.toInt, imageMeta.originalMime)

      val authors = imageAuthors.map(ia => Author(ia.typeAuthor, ia.name))
      val copyright = Copyright(licenseToLicenseDefinitionsMap.getOrElse(license, License(license, license, None)), origin, authors)


      var titles = List(ImageTitle(imageMeta.title, languageToISOMap.get(imageMeta.language)))
      var alttexts = List(ImageAltText(imageMeta.alttext, languageToISOMap.get(imageMeta.language)))
      translations.get(imageMeta.nid).foreach(_.foreach(translation => {
        titles = ImageTitle(translation.title, languageToISOMap.get(translation.language)) :: titles
        alttexts = ImageAltText(translation.alttext, languageToISOMap.get(translation.language)) :: alttexts
      }))

      val imageMetaInformation = ImageMetaInformation("0", titles, alttexts, ImageVariants(Option(thumb), Option(full)), copyright, tags)

      if(!imageMetaStore.containsExternalId(imageMeta.nid)) {
        if(!imageStorage.contains(thumbKey)) imageStorage.uploadFromUrl(thumb, thumbKey, sourceUrlThumb)
        if(!imageStorage.contains(fullKey)) imageStorage.uploadFromUrl(full, fullKey, sourceUrlFull)

        imageMetaStore.insert(imageMetaInformation, imageMeta.nid)
        inserted += 1
        println("INSERT: " + imageMeta.nid + " (" + imageMeta.title  + ", " + sourceUrlFull + ") -- " + (System.currentTimeMillis - start) + "ms")
      } else {
        imageMetaStore.update(imageMetaInformation, imageMeta.nid)
        updated += 1
        println("UPDATE: " + imageMeta.nid + " (" + imageMeta.title  + ", " + sourceUrlFull + ") -- " + (System.currentTimeMillis - start) + "ms")
      }
    } catch {
      case e: Exception  => {
        e.printStackTrace()
        failed += imageMeta.nid -> s"${imageMeta.nid} failed after ${System.currentTimeMillis - start}ms with error: ${e.getMessage}"
      }
    }

    Thread.sleep(250)
  }

  def toImageMeta(line: String): ImageMeta = {
    val x = line.split("#!#") match {
      case Array(a, b, c, d, e, f, g, h, i, j, k, l) => (a, b, c, d, e, f, g, h, i, j, k, l)
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

  def test() = {
    val lines = List("145610#!#Fotograf#!#NULL", "145611#!#NULL#!#Kenneth")
    val imageAuthor = lines
      .map(line => toImageAuthor(line)).collect{case Some(x) => x}
      .map(author => author.nid -> author)
      .groupBy(_._1).map { case (k,v) => (k,v.map(_._2))}


    imageAuthor.foreach(println)
  }
}



case class ImageMeta(nid:String, tnid:String, language:String, title:String, alttext:String, changed:String, originalFile:String, originalMime: String, originalSize: String, thumbFile:String, thumbMime:String, thumbSize: String)
case class ImageLicense(nid:String, license:String)
case class ImageAuthor(nid: String, typeAuthor:String, name:String)
case class ImageOrigin(nid: String, origin:String)