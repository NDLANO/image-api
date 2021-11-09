/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class V7__TranslateUntranslatedAuthorsTest extends UnitSuite with TestEnvironment {

  val migration = new V7__TranslateUntranslatedAuthors
  implicit val formats = org.json4s.DefaultFormats

  test("That redaksjonelt is translated to editorial whilst still keeping correct authors") {
    val metaString =
      """{"size": 87996, "tags": [{"tags": ["fordøyelsen", "fordøyelseskanalen", "fordøyelsesorganer"], "language": "nb"}, {"tags": ["digestion", "digestive tract", "digestive organs"], "language": "und"}, {"tags": ["digestion", "digestive tract", "digestive organs"], "language": "en"}], "titles": [{"title": "Tykktarmen ", "language": "unknown"}], "updated": "2017-12-12T12:11:19Z", "alttexts": [{"alttext": "Tykktarmen. Illustrasjon.", "language": "unknown"}], "captions": [], "imageUrl": "/Tykktarm%20med%20oppgaver.jpg", "copyright": {"origin": "", "license": {"url": "https://creativecommons.org/licenses/by-nc-sa/2.0/", "license": "by-nc-sa", "description": "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic"}, "creators": [{"name": "Mariana Ruiz Villarreal", "type": "Originator"}, {"name": "Bjørg E. B. Aurebekk", "type": "Redaksjonelt"}], "processors": [], "rightsholders": [{"name": "Amendor", "type": "Supplier"}]}, "updatedBy": "swagger-client", "contentType": "image/jpeg"}"""
    val result = migration.updateAuthorFormat(5, metaString)

    result.copyright.creators should equal(
      List(V5_Author("Originator", "Mariana Ruiz Villarreal"), V5_Author("Editorial", "Bjørg E. B. Aurebekk")))
    result.copyright.processors should equal(List.empty)
    result.copyright.rightsholders should equal(List(V5_Author("Supplier", "Amendor")))

  }

}
