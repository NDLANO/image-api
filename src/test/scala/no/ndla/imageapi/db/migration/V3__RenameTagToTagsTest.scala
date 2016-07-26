/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import db.migration.{V3_DBImage, V3__RenameTagToTags}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V3__RenameTagToTagsTest extends UnitSuite with TestEnvironment {
  val migration = new V3__RenameTagToTags()

  test("That convertingToNewFormat with no tags returns empty taglist") {
    val image = V3_DBImage(1,"""{"tags":[]}""")
    val optConverted = migration.convertTagsToNewFormat(image)

    optConverted.isDefined should be(true)
    optConverted.get.metadata should equal(image.metadata)
  }

  test("That converting an already converted image returns None") {
    val image = V3_DBImage(2,"""{"tags":[{"tags": ["eple", "banan"], "language": "nb"}, {"tags": ["apple", "banana"], "language": "en"}]}""")
    migration.convertTagsToNewFormat(image) should be(None)
  }

  test("That convertingToNewFormat converts to expected format") {
    val before = """{"tags":[{"tag":["eple","banan"],"language":"nb"},{"tag":["apple","banana"],"language":"en"}]}"""
    val expectedAfter = """{"tags":[{"tags":["eple","banan"],"language":"nb"},{"tags":["apple","banana"],"language":"en"}]}"""
    val image = V3_DBImage(3, before)

    val optConverted = migration.convertTagsToNewFormat(image)
    optConverted.isDefined should be(true)
    optConverted.get.metadata should equal(expectedAfter)
  }
}
