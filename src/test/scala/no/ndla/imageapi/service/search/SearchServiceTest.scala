/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import no.ndla.imageapi.{TestEnvironment, UnitSuite}

import scala.util.Success

class SearchServiceTest extends UnitSuite with TestEnvironment {

  override val imageSearchService = new ImageSearchService

  test("That createEmptyIndexIfNoIndexesExist never creates empty index if an index already exists") {
    when(imageIndexService.findAllIndexes(any[String])).thenReturn(Success(Seq("index1")))
    imageSearchService.createEmptyIndexIfNoIndexesExist()
    verify(imageIndexService, never).createIndexWithName(any[String])
  }

  test("That createEmptyIndexIfNoIndexesExist creates empty index if no indexes already exists") {
    when(imageIndexService.findAllIndexes(any[String])).thenReturn(Success(List.empty))
    when(imageIndexService.createIndexWithGeneratedName).thenReturn(Success("images-123j"))
    imageSearchService.createEmptyIndexIfNoIndexesExist()
    verify(imageIndexService).createIndexWithGeneratedName
  }

}
