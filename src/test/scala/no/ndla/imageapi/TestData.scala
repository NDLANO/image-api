package no.ndla.imageapi

import model._
import no.ndla.imageapi.integration.AmazonIntegration

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object TestData {

  val elg = ImageMetaInformation("1","Elg i busk",
    ImageVariants(Image("http://api.test.ndla.no/images/thumbs/Elg.jpg", "8680"), Image("http://api.test.ndla.no/images/full/Elg.jpg", "2865539")),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List("rovdyr", "elg"))

  val bjorn = ImageMetaInformation("2","Bjørn i busk",
    ImageVariants(Image("http://api.test.ndla.no/images/thumbs/Bjørn.jpg", "5958"), Image("http://api.test.ndla.no/images/full/Bjørn.jpg", "141134")),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List("rovdyr", "bjørn"))

  val jerv = ImageMetaInformation("3","Jerv på stein",
    ImageVariants(Image("http://api.test.ndla.no/images/thumbs/Jerv.jpg", "4834"), Image("http://api.test.ndla.no/images/full/Jerv.jpg", "39061")),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List("rovdyr", "jerv"))

  val mink = ImageMetaInformation("4","Overrasket mink",
    ImageVariants(Image("http://api.test.ndla.no/images/thumbs/Mink.jpg", "6875"), Image("http://api.test.ndla.no/images/full/Mink.jpg", "102559")),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List("rovdyr", "mink"))

  val rein = ImageMetaInformation("5","Rein har fanget rødtopp",
    ImageVariants(Image("http://api.test.ndla.no/images/thumbs/Rein.jpg", "7224"), Image("http://api.test.ndla.no/images/full/Rein.jpg", "504911")),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List("rovdyr", "rein", "jakt"))

  val nonexisting = ImageMetaInformation("6","Krokodille på krok",
    ImageVariants(Image("http://api.test.ndla.no/images/thumbs/Krokodille.jpg", "8680"), Image("http://api.test.ndla.no/images/full/Krokodille.jpg", "2865539")),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List("rovdyr", "krokodille"))

  val nonexistingWithoutThumb = ImageMetaInformation("6","Bison på sletten",
    ImageVariants(small = null, full = Image("http://api.test.ndla.no/images/full/Bison.jpg", "2865539")),
    Copyright("by-nc-sa", "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen"))),
    List("bison"))

  val testdata = List(elg,bjorn, jerv, mink, rein)

  def uploadTestdata = {
    val imageMeta = AmazonIntegration.getImageMetaWithDefaultCredentials()

    if (!imageMeta.exists) imageMeta.create
    testdata.foreach(imageMeta.upload(_))
  }
}
