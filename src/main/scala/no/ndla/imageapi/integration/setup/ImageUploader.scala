package no.ndla.imageapi.integration.setup

import model.Image
import no.ndla.imageapi.integration.AmazonIntegration

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object ImageUploader {

  val LocalImageDirectory = "/Users/kes/sandboxes/ndla/image-api/src/main/resources/images/"
  val imageBucket = AmazonIntegration.getImageBucket()
  val imageMeta = AmazonIntegration.getImageMeta()

  def createResources = {
    if (!imageMeta.exists) imageMeta.create
    if (!imageBucket.exists) imageBucket.create
  }

  def uploadImage(image: Image) = {
    imageBucket.upload(image, LocalImageDirectory)
    imageMeta.upload(image)
  }

}
