/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

import no.ndla.imageapi.integration.{ImageAuthor, ImageMeta, MainImageImport}
import no.ndla.imageapi.model.domain._
import org.joda.time.{DateTime, DateTimeZone}

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object TestData {

  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val ByNcSa = License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/"))

  val elg = ImageMetaInformation(Some(1), List(ImageTitle("Elg i busk", "nob")),List(ImageAltText("Elg i busk", "nob")),
    "Elg.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "elg"), "nob")), List(ImageCaption("Elg i busk", "nob")), "ndla124", updated())

  val bjorn = ImageMetaInformation(Some(2), List(ImageTitle("Bjørn i busk", "nob")),List(ImageAltText("Elg i busk", "nob")),
    "Bjørn.jpg", 141134, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "bjørn"), "nob")), List(ImageCaption("Bjørn i busk", "nob")), "ndla124", updated())

  val jerv = ImageMetaInformation(Some(3), List(ImageTitle("Jerv på stein", "nob")),List(ImageAltText("Elg i busk", "nob")),
    "Jerv.jpg", 39061, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "jerv"), "nob")), List(ImageCaption("Jerv på stein", "nob")), "ndla124", updated())

  val mink = ImageMetaInformation(Some(4), List(ImageTitle("Overrasket mink", "nob")),List(ImageAltText("Elg i busk", "nob")),
    "Mink.jpg", 102559, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "mink"), "nob")), List(ImageCaption("Overrasket mink", "nob")), "ndla124", updated())

  val rein = ImageMetaInformation(Some(5), List(ImageTitle("Rein har fanget rødtopp", "nob")),List(ImageAltText("Elg i busk", "nob")),
    "Rein.jpg", 504911, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "rein", "jakt"), "nob")), List(ImageCaption("Rein har fanget rødtopp", "nob")), "ndla124", updated())

  val nonexisting = ImageMetaInformation(Some(6), List(ImageTitle("Krokodille på krok", "nob")),List(ImageAltText("Elg i busk", "nob")),
    "Krokodille.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "krokodille"), "nob")), List(ImageCaption("Krokodille på krok", "nob")), "ndla124", updated())

  val nonexistingWithoutThumb = ImageMetaInformation(Some(6), List(ImageTitle("Bison på sletten", "nob")),List(ImageAltText("Elg i busk", "nob")),
    "Bison.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("bison"), "nob")), List(ImageCaption("Bison på sletten", "nob")), "ndla124", updated())

  val testdata = List(elg, bjorn, jerv, mink, rein)

  case class DiskImage(filename: String) extends ImageStream {
    override def contentType: String = s"image/$format"
    override def stream: InputStream = getClass.getResourceAsStream(s"/$filename")
    override def fileName: String = filename

    override lazy val sourceImage: BufferedImage = ImageIO.read(stream)
  }

  val NdlaLogoImage = DiskImage("ndla_logo.jpg")
  val ChildrensImage = DiskImage("children-drawing-582306_640.jpg") // From https://pixabay.com/en/children-drawing-home-tree-meadow-582306/

  val migrationImageMeta = ImageMeta("1234", "1234", "nob", "Elg i busk", Some("Busk i elg"), "2017-10-01 21:45:37.0", "Elg.jpg", "image/jpg", "1024", Some("I busk elg"))
  val migrationImageElg = MainImageImport(
    migrationImageMeta,
    List(ImageAuthor("Fotograf", "Test Testesen")),
    Some("by-nc-sa"),
    Some("http://www.scanpix.no"),
    List.empty
  )
}
