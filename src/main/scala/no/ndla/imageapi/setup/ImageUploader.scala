package no.ndla.imageapi.setup

import model.{Image, ImageData}
import no.ndla.imageapi.integration.{AmazonIntegration, AmazonImageMeta}

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object ImageUploader {

  val LocalImageDirectory = "/Users/kes/sandboxes/ndla/image-api/src/main/resources/images/"
  val imageBucket = AmazonIntegration.getImageBucket()
  val imageMeta = AmazonIntegration.getImageMeta()

  def main(args: Array[String]) {
    if (!imageMeta.exists) imageMeta.create
    if (!imageBucket.exists) imageBucket.create

    ImageData.alle.filter(!imageBucket.contains(_)).foreach(uploadImage(_))
  }

  def uploadImage(image: Image) = {
    imageBucket.upload(image, LocalImageDirectory)
    imageMeta.upload(image)
  }

}
