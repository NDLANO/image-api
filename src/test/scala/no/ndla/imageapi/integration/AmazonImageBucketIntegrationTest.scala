package no.ndla.imageapi.integration

import model.Image
import no.ndla.imageapi.{IntegrationTest, UnitSpec}

class AmazonImageBucketIntegrationTest extends UnitSpec {

  val existingImage = Image("1", "test", "test", "full/Elg.jpg", List("test", "test"))
  val nonExistingImage = Image("1", "test", "test", "full/dettefinnesikke.jpg", List("test", "test"))

  "ImageBucket.contains" should "return true for existing image" taggedAs (IntegrationTest) in {
    assert(true == AmazonIntegration.getImageBucket().contains(existingImage), "Check that image " + existingImage.imagePath + " is present in S3")
  }

  it should "return false for non-existing image" taggedAs (IntegrationTest) in {
    assert(false == AmazonIntegration.getImageBucket().contains(nonExistingImage), "Check that image " + existingImage.imagePath + " is NOT present in S3")
  }

}
