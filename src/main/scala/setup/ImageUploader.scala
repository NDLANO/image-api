package setup

import model.{Image, ImageData}

/**
 * Testklasse (og kanskje et utgangspunkt for en mer permanent løsning) som
 * kan benyttes til å laste opp bilder til en S3-bucket, samt metainformasjon til en DynamoDB-instans
 */
object ImageUploader {

  val ImageBucketName = "ndla-image"
  val ImageMetaName = "ndla-image-meta"
  val LocalImageDirectory = "/Users/kes/sandboxes/ndla/image-api/src/main/resources/images/"

  val imageMeta = new ImageMeta(ImageMetaName, ImageBucketName)
  val imageBucket = new ImageBucket(ImageBucketName, LocalImageDirectory)

  def main(args: Array[String]) {
    if (!imageMeta.exists) imageMeta.create
    if (!imageBucket.exists) imageBucket.create

    ImageData.alle.filter(!imageBucket.contains(_)).foreach(uploadImage(_))
  }

  def uploadImage(image: Image) = {
    imageBucket.upload(image)
    imageMeta.upload(image)
  }

}
