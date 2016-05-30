package no.ndla.imageapi.integration

import no.ndla.imageapi.{ComponentRegistry, IntegrationTest, UnitSuite}

class AmazonImageMetaIntegrationTest extends UnitSuite {

  val existingId = "1"
  val nonExistingId = "-1"
  val imageRepository = ComponentRegistry.imageRepository

  "ImageMeta.withId" should "return an image for an existing id" taggedAs (IntegrationTest) in {
    assert(imageRepository.withId(existingId).isDefined)
  }

  it should "not return an image for an non-existing id" taggedAs (IntegrationTest) in {
    assert(imageRepository.withId(nonExistingId).isEmpty)
  }
}
