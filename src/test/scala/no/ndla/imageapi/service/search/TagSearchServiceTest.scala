/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import no.ndla.imageapi.{ImageApiProperties, TestData, TestEnvironment, UnitSuite}
import no.ndla.imageapi.integration.Elastic4sClientFactory
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.domain.Sort
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome

import scala.util.Success

class TagSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment {

  override val e4sClient = Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val tagSearchService = new TagSearchService
  override val tagIndexService = new TagIndexService
  override val converterService = new ConverterService
  override val searchConverterService = new SearchConverterService

  val image1 = TestData.elg.copy(
    tags = Seq(
      domain.ImageTag(
        Seq("test", "testing", "testemer"),
        "nb"
      )
    )
  )

  val image2 = TestData.elg.copy(
    tags = Seq(
      domain.ImageTag(
        Seq("test"),
        "en"
      )
    )
  )

  val image3 = TestData.elg.copy(
    tags = Seq(
      domain.ImageTag(
        Seq("hei", "test", "testing"),
        "nb"
      ),
      domain.ImageTag(
        Seq("test"),
        "en"
      )
    )
  )

  val image4 = TestData.elg.copy(
    tags = Seq(
      domain.ImageTag(
        Seq("kyllingfilet", "filetkylling"),
        "nb"
      )
    )
  )

  val imagesToIndex = Seq(image1, image2, image3, image4)

  override def beforeAll(): Unit = if (elasticSearchContainer.isSuccess) {
    val indexName = tagIndexService.createIndexWithGeneratedName
    val alias = tagIndexService.updateAliasTarget(None, indexName.get)

    imagesToIndex.foreach(a => {
      val x = tagIndexService.indexDocument(a)
      x
    })

    val allTagsToIndex = imagesToIndex.flatMap(_.tags)
    val groupedByLanguage = allTagsToIndex.groupBy(_.language)
    val tagsDistinctByLanguage = groupedByLanguage.values.flatMap(x => x.flatMap(_.tags).toSet)

    blockUntil(() => tagSearchService.countDocuments() == tagsDistinctByLanguage.size)
  }

  def blockUntil(predicate: () => Boolean): Unit = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable => println("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting for predicate")
  }

  test("That searching for tags returns sensible results") {
    val Success(result) = tagSearchService.matchingQuery("test", "nb", 1, 100, Sort.ByRelevanceDesc)

    result.totalCount should be(3)
    result.results should be(Seq("test", "testemer", "testing"))
  }

  test("That only prefixes are matched") {
    val Success(result) = tagSearchService.matchingQuery("kylling", "nb", 1, 100, Sort.ByRelevanceDesc)

    result.totalCount should be(1)
    result.results should be(Seq("kyllingfilet"))
  }

}
