package no.ndla.imageapi.db.migration

import db.migration.{V6__ChangeImageStructure, V6_ImageJson}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V6_ChangeImageStructureTest  extends UnitSuite with TestEnvironment {
  val migration = new V6__ChangeImageStructure()
  val beforeImageMetaData = """{"images":{"full":{"url":"full/severdighetssymbol.jpg","size":3992,"contentType":"image/jpeg"},"small":{"url":"thumbs/severdighetssymbol.jpg","size":3021,"contentType":"image/jpeg"}}}"""
  val afterImageMetaData = """{"url":"full/severdighetssymbol.jpg","size":3992,"contentType":"image/jpeg"}"""

  test("That already converted image meta information is not converted") {
    val afterImage = V6_ImageJson(1, afterImageMetaData)
    migration.removeImageVariants(afterImage) should be(None)
  }
  test("That removeImageVariants removes image variants and the new is correct") {
    val beforeImage = V6_ImageJson(1, beforeImageMetaData)

    val optConverted = migration.removeImageVariants(beforeImage)
    optConverted.isDefined should be(true)
    optConverted.get.metadata should equal(afterImageMetaData)
  }
}