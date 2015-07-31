package no.ndla.imageapi.integration

import no.ndla.imageapi.{IntegrationTest, UnitSpec}

class AmazonImageMetaIntegrationTest extends UnitSpec {

  val existingId = "1"
  val nonExistingId = "-1"

  "ImageMeta.withId" should "return an image for an existing id" taggedAs (IntegrationTest) in {
    assert(AmazonIntegration.getImageMeta().withId(existingId).isDefined)
  }

  it should "not return an image for an non-existing id" taggedAs (IntegrationTest) in {
    assert(AmazonIntegration.getImageMeta().withId(nonExistingId).isEmpty)
  }
}
