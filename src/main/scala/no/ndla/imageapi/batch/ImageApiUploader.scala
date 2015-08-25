package no.ndla.imageapi.batch

import no.ndla.imageapi.model._
import no.ndla.imageapi.integration.AmazonIntegration

import scala.io.Source


object ImageApiUploader {

  def main(args: Array[String]) {

    new ImageApiUploader(maxUploads = 600,
      imageMetaFile = "/Users/kes/sandboxes/ndla/data-dump/20150812_1351/imagemetastest.csv",
      licensesFile = "/Users/kes/sandboxes/ndla/data-dump/20150812_1351/license_definition.csv",
      authorsFile = "/Users/kes/sandboxes/ndla/data-dump/20150812_1351/authors_definition.csv",
      originFile = "/Users/kes/sandboxes/ndla/data-dump/20150812_1351/origin_definition.csv")
      .uploadImages()
  }

}

/**
 *
 * @param maxUploads Max antall opplastinger som skal gjøres i kjøringen.
 *                   
 * @param imageMetaFile Fil som inneholder metainformasjon for bilder.
 *                      Kan produseres av følgende sql:
 *                      select n.nid, n.title, from_unixtime(n.changed), f.filepath as original, f.filemime as original_mime, f.filesize as original_size,
 *                        f2.filepath as thumb, f2.filemime as thumb_mime, f2.filesize as thumb_size
 *                      from node n
 *                        left join image i on (n.nid = i.nid)
 *                        left join image i2 on (n.nid = i2.nid)
 *                        left join files f on (i.fid = f.fid)
 *                        left join files f2 on (i2.fid = f2.fid)
 *                      where n.type = "image" and n.status = 1
 *                        and i.image_size = "_original"
 *                        and i2.image_size = "thumbnail"
 *                      order by n.nid desc limit 40000;
 *
 *
 * @param licensesFile Fil som inneholder lisensinformasjon om bilder
 *                     Kan produseres med følgende sql:
 *                     select n.nid, cc.license from node n
 *                     left join creativecommons_lite cc on (n.nid = cc.nid)
 *                     where n.type = "image" and n.status = 1;
 *
 * @param authorsFile Fil som inneholder authors for bilder.
 *                    Kan produseres av følgende sql:
 *                    SELECT n.nid AS image_nid, td.name AS author_type, person.title AS author FROM node n
 *                    LEFT JOIN ndla_authors na ON n.vid = na.vid
 *                    LEFT JOIN term_data td ON na.tid = td.tid
 *                    LEFT JOIN node person ON person.nid = na.person_nid
 *                    WHERE n.type = 'image' and n.status = 1 LIMIT 80000;
 *
 * @param originFile Fil som inneholder origins for bilder.
 *                   Kan produseres av følgende sql:
 *                   select n.nid, url.field_url_url from node n
 *                   left join content_field_url url ON url.vid = n.vid
 *                   where n.type = 'image' and n.status = 1 and url.field_url_url is not null
 *                   limit 40000
 */
class ImageApiUploader(maxUploads:Int = 1, imageMetaFile: String, licensesFile: String, authorsFile: String, originFile: String) {

  val UrlPrefix = "http://cm.test.ndla.no/"

  val imageMeta = Source.fromFile(imageMetaFile).getLines
    .map(line => toImageMeta(line))
    .toList

  val imageLicense = Source.fromFile(licensesFile).getLines
    .map(line => toImageLicense(line))
    .map(license => license.nid -> license)
    .toMap

  val imageOrigin = Source.fromFile(originFile).getLines()
    .map(line => toImageOrigin(line))
    .map(origin => origin.nid -> origin)
    .toMap

  val imageAuthor = Source.fromFile(authorsFile).getLines
    .map(line => toImageAuthor(line))
    .map(author => author.nid -> author)
    .toList.groupBy(_._1).map { case (k,v) => (k,v.map(_._2))}

  val imageStorage = AmazonIntegration.getImageStorageDefaultCredentials();
  val imageMetaStore = AmazonIntegration.getImageMeta()

  def uploadImages() = {
    imageMeta.take(maxUploads).foreach(upload(_))
  }

  def upload(imageMeta: ImageMeta) = {
    if(!imageMetaStore.containsExternalId(imageMeta.nid)) {
      val license = imageLicense.getOrElse(imageMeta.nid, ImageLicense("", "")).license
      val origin = imageOrigin.getOrElse(imageMeta.nid, ImageOrigin("", "")).origin
      val imageAuthors = imageAuthor.getOrElse(imageMeta.nid, List())
      val sourceUrlFull = UrlPrefix + imageMeta.originalFile
      val sourceUrlThumb = UrlPrefix + imageMeta.thumbFile
      val tags = Tags.forImage(imageMeta.nid)

      val thumbKey = imageMeta.thumbFile.replace("sites/default/files/images/", "thumbs/")
      val thumb = Image("http://api.test.ndla.no/images/" + thumbKey, imageMeta.thumbSize.toInt, imageMeta.thumbMime)

      val fullKey = imageMeta.originalFile.replace("sites/default/files/images/", "full/")
      val full = Image("http://api.test.ndla.no/images/" + fullKey, imageMeta.originalSize.toInt, imageMeta.originalMime)

      val authors = imageAuthors.map(ia => Author(ia.typeAuthor, ia.name))
      val copyright = Copyright(license, origin, authors)

      //TODO: Skriv om til å også ha med andre språk
      val imageMetaInformation = ImageMetaInformation("0", List(ImageTitle(imageMeta.title, "nob")), ImageVariants(Option(thumb), Option(full)), copyright, tags.map(ImageTag(_, "nob")))

      if(!imageStorage.contains(thumbKey)) imageStorage.uploadFromUrl(thumb, thumbKey, sourceUrlThumb)
      if(!imageStorage.contains(fullKey)) imageStorage.uploadFromUrl(full, fullKey, sourceUrlFull)

      imageMetaStore.upload(imageMetaInformation, imageMeta.nid)

      println("Uploaded:  " + imageMeta.nid + " (" + imageMeta.title  + ") with license " + license + " and authors " + authors.map(_.name) + ", full: " + sourceUrlFull + ", thumb: " + sourceUrlThumb + " with tags " + tags)

      Thread.sleep(1000)
    } else {
      println("Already exists: " + imageMeta.nid + ". Did not upload.")
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

  def toImageAuthor(line: String): ImageAuthor = {
    val x = line.split("#!#") match {
      case Array(a, b, c) => (a, b, c)
    }
    (ImageAuthor.apply _).tupled(x)
  }
}



case class ImageMeta(nid:String, title:String, changed:String, originalFile:String, originalMime: String, originalSize: String, thumbFile:String, thumbMime:String, thumbSize: String)
case class ImageLicense(nid:String, license:String)
case class ImageAuthor(nid: String, typeAuthor:String, name:String)
case class ImageOrigin(nid: String, origin:String)