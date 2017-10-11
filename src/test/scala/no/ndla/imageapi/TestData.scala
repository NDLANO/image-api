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

import no.ndla.imageapi.model.domain._
import org.joda.time.{DateTime, DateTimeZone}

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object TestData {

  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

  val ByNcSa = License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/"))

  val elg = ImageMetaInformation(Some(1), List(ImageTitle("Elg i busk", "nb")),List(ImageAltText("Elg i busk", "nb")),
    "Elg.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "elg"), "nb")), List(ImageCaption("Elg i busk", "nb")), "ndla124", updated())

  val bjorn = ImageMetaInformation(Some(2), List(ImageTitle("Bjørn i busk", "nb")),List(ImageAltText("Elg i busk", "nb")),
    "Bjørn.jpg", 141134, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "bjørn"), "nb")), List(ImageCaption("Bjørn i busk", "nb")), "ndla124", updated())

  val jerv = ImageMetaInformation(Some(3), List(ImageTitle("Jerv på stein", "nb")),List(ImageAltText("Elg i busk", "nb")),
    "Jerv.jpg", 39061, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "jerv"), "nb")), List(ImageCaption("Jerv på stein", "nb")), "ndla124", updated())

  val mink = ImageMetaInformation(Some(4), List(ImageTitle("Overrasket mink", "nb")),List(ImageAltText("Elg i busk", "nb")),
    "Mink.jpg", 102559, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "mink"), "nb")), List(ImageCaption("Overrasket mink", "nb")), "ndla124", updated())

  val rein = ImageMetaInformation(Some(5), List(ImageTitle("Rein har fanget rødtopp", "nb")),List(ImageAltText("Elg i busk", "nb")),
    "Rein.jpg", 504911, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "rein", "jakt"), "nb")), List(ImageCaption("Rein har fanget rødtopp", "nb")), "ndla124", updated())

  val nonexisting = ImageMetaInformation(Some(6), List(ImageTitle("Krokodille på krok", "nb")),List(ImageAltText("Elg i busk", "nb")),
    "Krokodille.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "krokodille"), "nb")), List(ImageCaption("Krokodille på krok", "nb")), "ndla124", updated())

  val nonexistingWithoutThumb = ImageMetaInformation(Some(6), List(ImageTitle("Bison på sletten", "nb")),List(ImageAltText("Elg i busk", "nb")),
    "Bison.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("bison"), "nb")), List(ImageCaption("Bison på sletten", "nb")), "ndla124", updated())

  val testdata = List(elg, bjorn, jerv, mink, rein)

  case class DiskImage(filename: String) extends ImageStream {
    override def contentType: String = s"image/$format"
    override def stream: InputStream = getClass.getResourceAsStream(s"/$filename")
    override def fileName: String = filename

    override lazy val sourceImage: BufferedImage = ImageIO.read(stream)
  }

  val NdlaLogoImage = DiskImage("ndla_logo.jpg")
  val ChildrensImage = DiskImage("children-drawing-582306_640.jpg") // From https://pixabay.com/en/children-drawing-home-tree-meadow-582306/
}
