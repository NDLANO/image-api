/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.Success

class SearchServiceTest extends UnitSuite with TestEnvironment {

  override val searchService = new SearchService

  test("That createEmptyIndexIfNoIndexesExist never creates empty index if an index already exists") {
    when(indexService.findAllIndexes(any[String])).thenReturn(Success(Seq("index1")))
    searchService.createEmptyIndexIfNoIndexesExist()
    verify(indexBuilderService, never()).createEmptyIndex
  }

  test("That createEmptyIndexIfNoIndexesExist creates empty index if no indexes already exists") {
    when(indexService.findAllIndexes(any[String])).thenReturn(Success(List.empty))
    when(indexBuilderService.createEmptyIndex).thenReturn(Success(Some("images-123j")))
    searchService.createEmptyIndexIfNoIndexesExist()
    verify(indexBuilderService).createEmptyIndex
  }

}
