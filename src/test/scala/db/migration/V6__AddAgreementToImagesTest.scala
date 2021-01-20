/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.json4s.native.Serialization.{read, write}

class V6__AddAgreementToImagesTest extends UnitSuite with TestEnvironment {

  val migration = new V6__AddAgreementToImages
  implicit val formats = org.json4s.DefaultFormats

  test("That author is converted to new format correctly") {
    val oldFormatString =
      "{\"size\": 523885, \"tags\": [{\"tags\": [\"old\", \"fence\", \"black and white\"], \"language\": \"en\"}, {\"tags\": [\"gammel\", \"gjerde\", \"svart og hvit\"], \"language\": \"nb\"}, {\"tags\": [\"gamal\", \"gjerde\"], \"language\": \"nn\"}, {\"tags\": [\"giedtie\"], \"language\": \"se\"}, {\"tags\": [\"old\", \"fence\", \"black and white\"], \"language\": \"unknown\"}], \"titles\": [{\"title\": \"Old Fence\", \"language\": \"unknown\"}], \"updated\": \"2017-11-07T11:18:16Z\", \"alttexts\": [{\"alttext\": \"Old Fence. Picture. \", \"language\": \"unknown\"}], \"captions\": [], \"imageUrl\": \"/old%20fence.jpg\", \"copyright\": {\"origin\": \"\", \"authors\": [{\"name\": \"lu2shoot\", \"type\": \"Forfatter\"}, {\"name\": \"MrDogg\", \"type\": \"Distributør\"}], \"license\": {\"url\": \"https://creativecommons.org/licenses/by-sa/2.0/\", \"license\": \"by-sa\", \"description\": \"Creative Commons Attribution-ShareAlike 2.0 Generic\"}}, \"updatedBy\": \"content-import-client\", \"contentType\": \"image/jpeg\"}"
    val oldId = 1
    val before = read[V5_ImageMetaInformation](oldFormatString)
    V5_ImageMetaInformation(
      Some(oldId),
      before.titles,
      before.alttexts,
      before.imageUrl,
      before.size,
      before.contentType,
      before.copyright,
      before.tags,
      before.captions,
      before.updatedBy,
      before.updated
    )

    val after = migration.updateAuthorFormat(oldId, oldFormatString)

    after.titles should equal(before.titles)
    after.id.get should equal(oldId)
    after.copyright.creators should contain(V5_Author("Writer", "lu2shoot"))
    after.copyright.rightsholders should contain(V5_Author("Distributor", "MrDogg"))

  }

  test("That already updated format is not broken AND not updated by not having an id") {
    val oldFormatString =
      "{\"size\": 12801066, \"tags\": [], \"titles\": [{\"title\": \"Yoman\", \"language\": \"en\"}, {\"title\": \"hey\", \"language\": \"nb\"}], \"updated\": \"2017-11-07T13:05:43Z\", \"alttexts\": [{\"alttext\": \"test2\", \"language\": \"en\"}], \"captions\": [{\"caption\": \"captionheredude\", \"language\": \"en\"}], \"imageUrl\": \"/RzWfa7s2.png\", \"copyright\": {\"origin\": \"http://www.scanpix.no\", \"license\": {\"url\": \"https://creativecommons.org/licenses/by-nc-sa/2.0/\", \"license\": \"by-nc-sa\", \"description\": \"Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic\"}, \"creators\": [{\"name\": \"Maximilian Stock Ltd\", \"type\": \"Photographer\"}], \"processors\": [], \"rightsholders\": [{\"name\": \"StockFood\", \"type\": \"Supplier\"}, {\"name\": \"NTB scanpix\", \"type\": \"Supplier\"}]}, \"updatedBy\": \"swagger-client\", \"contentType\": \"image/png\"}"
    val oldId = 1
    val before = read[V6_ImageMetaInformation](oldFormatString)
    V6_ImageMetaInformation(
      Some(oldId),
      before.titles,
      before.alttexts,
      before.imageUrl,
      before.size,
      before.contentType,
      before.copyright,
      before.tags,
      before.captions,
      before.updatedBy,
      before.updated
    )

    val after = migration.updateAuthorFormat(oldId, oldFormatString)

    after.titles should equal(before.titles)
    after.id should equal(None)
    after.copyright.creators should contain(V5_Author("Photographer", "Maximilian Stock Ltd"))
    after.copyright.rightsholders should contain(V5_Author("Supplier", "StockFood"))
    after.copyright.rightsholders should contain(V5_Author("Supplier", "NTB scanpix"))
  }
}
