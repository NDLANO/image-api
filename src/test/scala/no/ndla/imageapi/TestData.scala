package no.ndla.imageapi

import no.ndla.imageapi.model._
import no.ndla.imageapi.integration.AmazonIntegration

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object TestData {

  val elg = ImageMetaInformation("1",List(ImageTitle("Elg i busk", "nb")),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Elg.jpg", 8680, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Elg.jpg", 2865539, "image/jpeg"))),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", "nb"), ImageTag("elg", "nb")))

  val bjorn = ImageMetaInformation("2", List(ImageTitle("Bjørn i busk", "nb")),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Bjørn.jpg", 5958, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Bjørn.jpg", 141134, "image/jpeg"))),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", "nb"), ImageTag("bjørn", "nb")))

  val jerv = ImageMetaInformation("3",List(ImageTitle("Jerv på stein", "nb")),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Jerv.jpg", 4834, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Jerv.jpg", 39061, "image/jpeg"))),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", "nb"), ImageTag("jerv", "nb")))

  val mink = ImageMetaInformation("4",List(ImageTitle("Overrasket mink", "nb")),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Mink.jpg", 6875, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Mink.jpg", 102559, "image/jpeg"))),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", "nb"), ImageTag("mink", "nb")))

  val rein = ImageMetaInformation("5",List(ImageTitle("Rein har fanget rødtopp", "nb")),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Rein.jpg", 7224, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Rein.jpg", 504911, "image/jpeg"))),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", "nb"), ImageTag("rein", "nb"), ImageTag("jakt", "nb")))

  val nonexisting = ImageMetaInformation("6",List(ImageTitle("Krokodille på krok", "nb")),
    ImageVariants(Option(Image("http://api.test.ndla.no/images/thumbs/Krokodille.jpg", 8680, "image/jpeg")), Option(Image("http://api.test.ndla.no/images/full/Krokodille.jpg", 2865539, "image/jpeg"))),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("rovdyr", "nb"), ImageTag("krokodille", "nb")))

  val nonexistingWithoutThumb = ImageMetaInformation("6",List(ImageTitle("Bison på sletten", "nb")),
    ImageVariants(small = None, full = Option(Image("http://api.test.ndla.no/images/full/Bison.jpg", 2865539, "image/jpeg"))),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List(ImageTag("bison", "nb")))

  val testdata = List(elg,bjorn, jerv, mink, rein)
}
