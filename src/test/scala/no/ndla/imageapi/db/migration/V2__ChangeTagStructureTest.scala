/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import db.migration.{V2_DBImage, V2__ChangeTagStructure}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V2__ChangeTagStructureTest extends UnitSuite with TestEnvironment {
  val migrator = new V2__ChangeTagStructure()

  test("That convertTagsToNewFormat with no tags returns empty taglist") {
    val image = V2_DBImage (1, """{"tags":[]}""")
    val optConverted = migrator.convertTagsToNewFormat(image)

    optConverted.isDefined should be(true)
    optConverted.get.metadata should equal(image.metadata)
  }

  test("That converting an already converted content node returns none") {
    val content = V2_DBImage(2,"""{"tags":[{"tag": ["eple", "banan"], "language": "nb"}, {"tag": ["apple", "banana"], "language": "en"}]}""")
    migrator.convertTagsToNewFormat(content) should be(None)
  }

  test("That convertTagsToNewFormat converts to expected format") {
    val before = """{"tags": [{"tag": "eple", "language":"nb"}, {"tag": "banan", "language":"nb"}, {"tag": "apple", "language":"en"}, {"tag": "banana", "language":"en"}]}"""
    val expectedAfter = """{"tags":[{"tag":["eple","banan"],"language":"nb"},{"tag":["apple","banana"],"language":"en"}]}"""
    val image = V2_DBImage(3, before)

    val optConverted = migrator.convertTagsToNewFormat(image)
    optConverted.isDefined should be(true)
    optConverted.get.metadata should equal(expectedAfter)
  }
}
