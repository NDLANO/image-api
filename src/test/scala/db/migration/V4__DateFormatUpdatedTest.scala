/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.imageapi.UnitSuite
import org.mockito.Mockito.when


class V4__DateFormatUpdatedTest extends UnitSuite {

  val migration = new V4__DateFormatUpdated

  ignore("fix broken updated date string"){
    val before = """{"size":381667,"tags":[{"tags":["perforator","perforering","perforeringskanal"],"language":"nb"},{"tags":["shaped charge"]},{"tags":["shaped charge"],"language":"en"}],"titles":[{"title":"Perforeringssekvens"}],"alttexts":[{"alttext":"Avfyrings av skudd i brønn. Illustrasjon."}],"captions":[],"imageUrl":"avfyringssekvens.jpg","copyright":{"origin":"","authors":[{"name":"Baker Hughes Inc","type":"Opphavsmann"}],"license":{"url":"https://creativecommons.org/licenses/by-sa/2.0/","license":"by-sa","description":"Creative Commons Attribution-ShareAlike 2.0 Generic"}},"contentType":"image/jpeg","updatedBy":"content-import-client","updated":"BARETULL"}"""
    val expectedAfter = """{}"""

//    when(migration.timeService.nowAsString()).thenReturn("2017-05-5T14:22:55+0200")

    val image = V4__DBImageMetaInformation(1, before)
    val converted = migration.fixImageUpdatestring(image)
    converted.document should equal(expectedAfter)

  }

}
