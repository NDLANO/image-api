/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V5__AddLanguageToAllTest extends UnitSuite with TestEnvironment {

  val migration = new V5__AddLanguageToAll

  test("add language to stuff with missing no language") {
    val before = V5_ImageMetaInformation(
      Some(1),
      Seq(V5_ImageTitle("Tittel", None)),
      Seq(V5_ImageAltText("Alttext", None)),
      "",
      0,
      "",
      null,
      Seq(V5_ImageTag(Seq("Tag"), None)),
      Seq(V5_ImageCaption("Caption", None)),
      "",
      null
    )

    val after = migration.updateImageLanguage(before)
    after.titles.forall(_.language.contains("und")) should be(true)
    after.alttexts.forall(_.language.contains("und")) should be(true)
    after.tags.forall(_.language.contains("und")) should be(true)
    after.captions.forall(_.language.contains("und")) should be(true)
  }

  test("add language to stuff with missing empty string as language") {
    val before = V5_ImageMetaInformation(
      Some(1),
      Seq(V5_ImageTitle("Tittel", Some(""))),
      Seq(V5_ImageAltText("Alttext", Some(""))),
      "",
      0,
      "",
      null,
      Seq(V5_ImageTag(Seq("Tag"), Some(""))),
      Seq(V5_ImageCaption("Caption", Some(""))),
      "",
      null
    )

    val after = migration.updateImageLanguage(before)
    after.titles.forall(_.language.contains("und")) should be(true)
    after.alttexts.forall(_.language.contains("und")) should be(true)
    after.tags.forall(_.language.contains("und")) should be(true)
    after.captions.forall(_.language.contains("und")) should be(true)
  }

  test("that not modifying an existing language") {
    val before = V5_ImageMetaInformation(
      Some(1),
      Seq(V5_ImageTitle("Tittel", Some("nb"))),
      Seq(V5_ImageAltText("Alttext", Some("en"))),
      "",
      0,
      "",
      null,
      Seq(V5_ImageTag(Seq("Tag"), Some("fr"))),
      Seq(V5_ImageCaption("Caption", Some("de"))),
      "",
      null
    )

    val after = migration.updateImageLanguage(before)
    after.titles.forall(_.language.contains("nb")) should be(true)
    after.alttexts.forall(_.language.contains("en")) should be(true)
    after.tags.forall(_.language.contains("fr")) should be(true)
    after.captions.forall(_.language.contains("de")) should be(true)
  }

}
