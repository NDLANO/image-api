package no.ndla.imageapi

import no.ndla.imageapi.model._

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object TestData {

  val ByNcSa = License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/"))

  val elg = ImageMetaInformation("1", "http://api.test.ndla.no/images/1", List(ImageTitle("Elg i busk", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Elg.jpg", 8680, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Elg.jpg", 2865539, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "elg"), Option("nb"))))

  val bjorn = ImageMetaInformation("2", "http://api.test.ndla.no/images/2", List(ImageTitle("Bjørn i busk", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Bjørn.jpg", 5958, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Bjørn.jpg", 141134, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "bjørn"), Option("nb"))))

  val jerv = ImageMetaInformation("3", "http://api.test.ndla.no/images/3", List(ImageTitle("Jerv på stein", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Jerv.jpg", 4834, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Jerv.jpg", 39061, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "jerv"), Option("nb"))))

  val mink = ImageMetaInformation("4", "http://api.test.ndla.no/images/4", List(ImageTitle("Overrasket mink", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Mink.jpg", 6875, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Mink.jpg", 102559, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "mink"), Option("nb"))))

  val rein = ImageMetaInformation("5", "http://api.test.ndla.no/images/5", List(ImageTitle("Rein har fanget rødtopp", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Rein.jpg", 7224, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Rein.jpg", 504911, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "rein", "jakt"), Option("nb"))))

  val nonexisting = ImageMetaInformation("6", "http://api.test.ndla.no/images/6", List(ImageTitle("Krokodille på krok", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Krokodille.jpg", 8680, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Krokodille.jpg", 2865539, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("rovdyr", "krokodille"), Option("nb"))))

  val nonexistingWithoutThumb = ImageMetaInformation("6", "http://api.test.ndla.no/images/6", List(ImageTitle("Bison på sletten", Option("nb"))),List(ImageAltText("Elg i busk", Option("nb"))),
    ImageVariants(small = None, full = Option(Image("http://api.test.ndla.no/images/full/Bison.jpg", 2865539, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag(List("bison"), Option("nb"))))

  val testdata = List(elg,bjorn, jerv, mink, rein)
}
