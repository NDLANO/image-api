/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class MappingApiClientTest extends UnitSuite with TestEnvironment {

  override val mappingApiClient = new MappingApiClient

  test("That memoization works..") {



  }
}
