/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import no.ndla.imageapi.UnitSuite

class V2__RemoveFullFromImagePathTest extends UnitSuite {
  val migration = new V2__RemoveFullFromImagePath

  test("convertImageUrl should convert to expected format") {
    val before = """{"imageUrl": "full/image.jpg"}"""
    val expectedAfter = """{"imageUrl":"image.jpg"}"""
    val learningPath = V2_DBImageMetaInformation(1, before)

    val converted = migration.convertImageUrl(learningPath)
    converted.document should equal(expectedAfter)
  }

}
