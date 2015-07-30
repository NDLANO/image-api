package setup

import dao.{ImageMeta, ImageBucket}
import model.{Image, ImageData}

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object ImageUploader {

  val LocalImageDirectory = "/Users/kes/sandboxes/ndla/image-api/src/main/resources/images/"

  def main(args: Array[String]) {
    if (!ImageMeta.exists) ImageMeta.create
    if (!ImageBucket.exists) ImageBucket.create

    ImageData.alle.filter(!ImageBucket.contains(_)).foreach(uploadImage(_))
  }

  def uploadImage(image: Image) = {
    ImageBucket.upload(image, LocalImageDirectory)
    ImageMeta.upload(image)
  }

}
