package no.ndla.imageapi

import no.ndla.imageapi.model._
import no.ndla.imageapi.integration.AmazonIntegration

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object TestData {

  val ByNcSa = License("by-nc-sa", "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc-sa/2.0/"))

  val elg = ImageMetaInformation("1",List(ImageTitle("Elg i busk", Option("nob"))),List(ImageAltText("Elg i busk", Option("nob"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Elg.jpg", 8680, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Elg.jpg", 2865539, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", Option("nob")), ImageTag("elg", Option("nob"))))

  val bjorn = ImageMetaInformation("2", List(ImageTitle("Bjørn i busk", Option("nob"))),List(ImageAltText("Elg i busk", Option("nob"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Bjørn.jpg", 5958, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Bjørn.jpg", 141134, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", Option("nob")), ImageTag("bjørn", Option("nob"))))

  val jerv = ImageMetaInformation("3",List(ImageTitle("Jerv på stein", Option("nob"))),List(ImageAltText("Elg i busk", Option("nob"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Jerv.jpg", 4834, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Jerv.jpg", 39061, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", Option("nob")), ImageTag("jerv", Option("nob"))))

  val mink = ImageMetaInformation("4",List(ImageTitle("Overrasket mink", Option("nob"))),List(ImageAltText("Elg i busk", Option("nob"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Mink.jpg", 6875, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Mink.jpg", 102559, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", Option("nob")), ImageTag("mink", Option("nob"))))

  val rein = ImageMetaInformation("5",List(ImageTitle("Rein har fanget rødtopp", Option("nob"))),List(ImageAltText("Elg i busk", Option("nob"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Rein.jpg", 7224, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Rein.jpg", 504911, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", Option("nob")), ImageTag("rein", Option("nob")), ImageTag("jakt", Option("nob"))))

  val nonexisting = ImageMetaInformation("6",List(ImageTitle("Krokodille på krok", Option("nob"))),List(ImageAltText("Elg i busk", Option("nob"))),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Krokodille.jpg", 8680, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Krokodille.jpg", 2865539, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", Option("nob")), ImageTag("krokodille", Option("nob"))))

  val nonexistingWithoutThumb = ImageMetaInformation("6",List(ImageTitle("Bison på sletten", Option("nob"))),List(ImageAltText("Elg i busk", Option("nob"))),
    ImageVariants(small = None, full = Option(Image("http://api.test.ndla.no/images/full/Bison.jpg", 2865539, "image/jpeg"))),
    Copyright(ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("bison", Option("nob"))))

  val testdata = List(elg,bjorn, jerv, mink, rein)
}
