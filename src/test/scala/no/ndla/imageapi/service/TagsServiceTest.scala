/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.domain.ImageTag
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class TagsServiceTest extends UnitSuite with TestEnvironment {
  val service = new TagsService

  test("getISO639 returns a iso639 language code for a valid language url") {
    service.getISO639("http://psi.some.url.org/#nob") should equal (Option("nb"))
    service.getISO639("http://psi.some.url.org/#nno") should equal (Option("nn"))
    service.getISO639("http://psi.some.url.org/#eng") should equal (Option("en"))
  }

  test("getISO639 returns None for an invalid language url") {
    service.getISO639("http://psi.some.url.org/#XXX") should equal (None)
    service.getISO639("http://psi.some.url.org/#YYY") should equal (None)
    service.getISO639("http://psi.some.url.org/#ZZZ") should equal (None)
  }

}
