/*
 * Part of NDLA image-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V8__UpdateLicensesTest extends UnitSuite with TestEnvironment {
  val migration = new V8__UpdateLicenses

  test("migration should update to new status format") {
    {
      val old =
        s"""{"copyright":{"license":{"license":"by","url":"http://trolol.ol","description":"huehue"},"creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":{"license":"by-sa","url":"http://trolol.ol","description":"huehue"},"creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-SA-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertDocument(old) should equal(expected)

    }
    {
      val old =
        s"""{"copyright":{"license":{"license":"by-nc-nd","url":"http://trolol.ol","description":"huehue"},"creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC-BY-NC-ND-4.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":{"license":"copyrighted"},"creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"COPYRIGHTED","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertDocument(old) should equal(expected)
    }
    {
      val old =
        s"""{"copyright":{"license":{"license":"cc0"},"creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      val expected =
        s"""{"copyright":{"license":"CC0-1.0","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""
      migration.convertDocument(old) should equal(expected)
    }
  }

  test("migration not do anything if the document already has new status format") {
    val original =
      s"""{"copyright":{"license":"COPYRIGHTED","creators":[{"name":"henrik","type":"Writer"}],"processors":[],"rightsholders":[]},"title":[{"title":"tittel","language":"nb"}]}"""

    migration.convertDocument(original) should equal(original)
  }
}
