package no.ndla.imageapi.integration

import no.ndla.imageapi.{IntegrationTest, TestData, UnitSpec}

class AmazonImageBucketIntegrationTest extends UnitSpec {

  val existingImage = TestData.elg
  val nonExistingImage = TestData.nonexisting

  "ImageBucket.contains" should "return true for existing image" taggedAs (IntegrationTest) in {
    assert(true == AmazonIntegration.getImageBucket().contains(existingImage), "Check that image " + existingImage.images.full.url + " is present in S3")
  }

  it should "return false for non-existing image" taggedAs (IntegrationTest) in {
    assert(false == AmazonIntegration.getImageBucket().contains(nonExistingImage), "Check that image " + nonExistingImage.images.full.url + " is NOT present in S3")
  }

}
