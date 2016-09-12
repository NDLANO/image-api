package no.ndla.imageapi.db.migration

import db.migration.{V4_DBImage, V4__AddCaptionsList}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V4__AddCaptionsListTest extends UnitSuite with TestEnvironment {
  val migration = new V4__AddCaptionsList()

  test("That converting an already converted image returns None") {
    val image = V4_DBImage(1,"""{"captions": []}""")
    migration.addCaptionsArray(image) should be(None)
  }

  test("That addCaptionsArray converts to expected format") {
    val before = """{}"""
    val expectedAfter = """{"captions":[]}"""
    val image = V4_DBImage(2, before)

    val optConverted = migration.addCaptionsArray(image)
    optConverted.isDefined should be(true)
    optConverted.get.metadata should equal(expectedAfter)
  }

  test("THat addCaptionsArray does not overwrite existing fields") {
    val before = """{"alttexts": []}"""
    val expectedAfter = """{"alttexts":[],"captions":[]}"""
    val image = V4_DBImage(3, before)

    val optConverted = migration.addCaptionsArray(image)
    optConverted.isDefined should be(true)
    optConverted.get.metadata should equal(expectedAfter)
  }
}
