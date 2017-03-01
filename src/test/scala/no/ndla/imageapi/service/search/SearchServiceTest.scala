/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.ImageApiProperties.{DefaultPageSize, MaxPageSize}
import no.ndla.imageapi.integration.JestClientFactory
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.{Node, NodeBuilder}
import no.ndla.network.ApplicationUrl
import no.ndla.tag.IntegrationTest
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.reflect.io.Path
import scala.util.Random

@IntegrationTest
class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esHttpPort = new Random(System.currentTimeMillis()).nextInt(30000 - 20000) + 20000
  val esDataDir = "esTestData"
  var esNode: Node = _

  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esHttpPort")
  override val searchConverterService = new SearchConverterService
  override val converterService = new ConverterService
  override val indexService = new IndexService
  override val searchService = new SearchService

  val getStartAtAndNumResults = PrivateMethod[(Int, Int)]('getStartAtAndNumResults)

  val largeImage = Image("large-full-url", 10000, "jpg")
  val smallImage = Image("small-full-url", 100, "jpg")

  val byNcSa = Copyright(License("by-nc-sa", "Attribution-NonCommercial-ShareAlike", None), "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright(License("publicdomain", "Public Domain", None), "Metropolis", List(Author("Forfatter", "Bruce Wayne")))

  val image1 = ImageMetaInformation(Some(1), List(ImageTitle("Batmen er på vift med en bil", Some("nb"))), List(ImageAltText("Bilde av en bil flaggermusmann som vifter med vingene bil.", Some("nb"))), largeImage.fileName, largeImage.size, largeImage.contentType, byNcSa, List(ImageTag(List("fugl"), Some("nb"))), List())
  val image2 = ImageMetaInformation(Some(2), List(ImageTitle("Pingvinen er ute og går", Some("nb"))), List(ImageAltText("Bilde av en en pingvin som vagger borover en gate.", Some("nb"))), largeImage.fileName, largeImage.size, largeImage.contentType, publicDomain, List(ImageTag(List("fugl"), Some("nb"))), List())
  val image3 = ImageMetaInformation(Some(3), List(ImageTitle("Donald Duck kjører bil", Some("nb"))), List(ImageAltText("Bilde av en en and som kjører en rød bil.", Some("nb"))), smallImage.fileName, smallImage.size, smallImage.contentType, byNcSa, List(ImageTag(List("and"), Some("nb"))), List())
  val image4 = ImageMetaInformation(Some(4), List(ImageTitle("Hulken er ute og lukter på blomstene", None)), Seq(), smallImage.fileName, smallImage.size, smallImage.contentType, byNcSa, Seq(), Seq())

  override def beforeAll() = {
    Path(esDataDir).deleteRecursively()
    val settings = Settings.settingsBuilder()
      .put("path.home", esDataDir)
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("http.port", esHttpPort)
      .put("cluster.name", getClass.getName)
      .build()

    esNode = new NodeBuilder().settings(settings).node()
    esNode.start()


    val indexName = indexService.createIndex().get
    indexService.updateAliasTarget(None, indexName)
    indexService.indexDocument(image1)
    indexService.indexDocument(image2)
    indexService.indexDocument(image3)
    indexService.indexDocument(image4)

    val servletRequest = mock[HttpServletRequest]
    when(servletRequest.getHeader(any[String])).thenReturn("http")
    when(servletRequest.getServerName).thenReturn("localhost")
    when(servletRequest.getServletPath).thenReturn("/image-api/v1/images/")
    ApplicationUrl.set(servletRequest)

    blockUntil(() => searchService.countDocuments() == 4)
  }

  override def afterAll() = {
    esNode.close()
    Path(esDataDir).deleteRecursively()
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService invokePrivate getStartAtAndNumResults(None, None) should equal((0, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService invokePrivate getStartAtAndNumResults(None, Some(1000)) should equal((0, MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService invokePrivate getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val pageSize = 43
    val expectedStartAt = (page - 1) * pageSize
    searchService invokePrivate getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val searchResult = searchService.all(None, None, None, None)
    searchResult.totalCount should be(4)
    searchResult.results.size should be(4)
    searchResult.page should be(1)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("4")
  }

  test("That all filtering on minimumsize only returns images larger than minimumsize") {
    val searchResult = searchService.all(Some(500), None, None, None)
    searchResult.totalCount should be(2)
    searchResult.results.size should be(2)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("2")
  }

  test("That all filtering on license only returns images with given license") {
    val searchResult = searchService.all(None, Some("publicdomain"), None, None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val searchResultPage1 = searchService.all(None, None, Some(1), Some(2))
    val searchResultPage2 = searchService.all(None, None, Some(2), Some(2))
    searchResultPage1.totalCount should be(4)
    searchResultPage1.page should be(1)
    searchResultPage1.pageSize should be(2)
    searchResultPage1.results.size should be(2)
    searchResultPage1.results.head.id should be("1")
    searchResultPage1.results.last.id should be("2")

    searchResultPage2.totalCount should be(4)
    searchResultPage2.page should be(2)
    searchResultPage2.pageSize should be(2)
    searchResultPage2.results.size should be(2)
    searchResultPage2.results.head.id should be("3")
    searchResultPage2.results.last.id should be("4")
  }

  test("That both minimum-size and license filters are applied.") {
    val searchResult = searchService.all(Some(500), Some("publicdomain"), None, None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches title and alttext ordered by relevance") {
    val searchResult = searchService.matchingQuery("bil", None, None, None, None, None)
    searchResult.totalCount should be(2)
    searchResult.results.size should be(2)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("3")
  }

  test("That search matches title") {
    val searchResult = searchService.matchingQuery("Pingvinen", None, Some("nb"), None, None, None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches tags") {
    val searchResult = searchService.matchingQuery("and", None, Some("nb"), None, None, None)
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("3")
  }

  test("That search matches alttext without specifying language") {
    val searchResult = searchService.matchingQuery("Bilde av en and", None, None, None, None, None)
    searchResult.totalCount should be (1)
    searchResult.results.size should be (1)
    searchResult.results.head.id should be ("3")
  }

  test("That search matches title with unknown language analyzed in Norwegian") {
    val searchResult = searchService.matchingQuery("blomst", None, None, None, None, None)
    searchResult.totalCount should be (1)
    searchResult.results.size should be (1)
    searchResult.results.head.id should be ("4")
  }

  def blockUntil(predicate: () => Boolean) = {
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
}
