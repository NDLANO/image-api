package no.ndla.imageapi.integration

import no.ndla.imageapi.{IntegrationTest, TestData, UnitSpec}

class AmazonImageStorageIntegrationTest extends UnitSpec {

  val existingImage = TestData.elg
  val nonExistingImage = TestData.nonexisting

  "ImageStorage.contains" should "return true for existing image" taggedAs (IntegrationTest) in {
    assert(true == AmazonIntegration.getImageStorage().contains(existingImage), "Check that image " + existingImage.images.full.url + " is present in S3")
  }

  it should "return false for non-existing image" taggedAs (IntegrationTest) in {
    assert(false == AmazonIntegration.getImageStorage().contains(nonExistingImage), "Check that image " + nonExistingImage.images.full.url + " is NOT present in S3")
  }

}
