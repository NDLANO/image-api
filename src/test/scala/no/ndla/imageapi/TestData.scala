/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import java.io.InputStream

import no.ndla.imageapi.model.domain._

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object TestData {

  val ByNcSa = License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/"))

  val elg = ImageMetaInformation(Some(1), List(ImageTitle("Elg i busk", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    "http://api.test.ndla.no/images/full/Elg.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "elg"), Option("nb"))), List(ImageCaption("Elg i busk", Some("nb"))))

  val bjorn = ImageMetaInformation(Some(2), List(ImageTitle("Bjørn i busk", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    "http://api.test.ndla.no/images/full/Bjørn.jpg", 141134, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "bjørn"), Option("nb"))), List(ImageCaption("Bjørn i busk", Some("nb"))))

  val jerv = ImageMetaInformation(Some(3), List(ImageTitle("Jerv på stein", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    "http://api.test.ndla.no/images/full/Jerv.jpg", 39061, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "jerv"), Option("nb"))), List(ImageCaption("Jerv på stein", Some("nb"))))

  val mink = ImageMetaInformation(Some(4), List(ImageTitle("Overrasket mink", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    "http://api.test.ndla.no/images/full/Mink.jpg", 102559, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "mink"), Option("nb"))), List(ImageCaption("Overrasket mink", Some("nb"))))

  val rein = ImageMetaInformation(Some(5), List(ImageTitle("Rein har fanget rødtopp", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    "http://api.test.ndla.no/images/full/Rein.jpg", 504911, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "rein", "jakt"), Option("nb"))), List(ImageCaption("Rein har fanget rødtopp", Some("nb"))))

  val nonexisting = ImageMetaInformation(Some(6), List(ImageTitle("Krokodille på krok", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    "http://api.test.ndla.no/images/full/Krokodille.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "krokodille"), Option("nb"))), List(ImageCaption("Krokodille på krok", Some("nb"))))

  val nonexistingWithoutThumb = ImageMetaInformation(Some(6), List(ImageTitle("Bison på sletten", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    "http://api.test.ndla.no/images/full/Bison.jpg", 2865539, "image/jpeg",
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("bison"), Option("nb"))), List(ImageCaption("Bison på sletten", Some("nb"))))

  val testdata = List(elg, bjorn, jerv, mink, rein)

  case class DiskImage(filename: String) extends ImageStream {
    override def contentType: String = s"image/$format"
    override def stream: InputStream = getClass.getResourceAsStream(s"/$filename")
    override def fileName: String = filename
  }

  val NdlaLogoImage = DiskImage("ndla_logo.jpg")
}
