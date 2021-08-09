/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import java.util.Date

import javax.servlet.http.HttpServletRequest
import no.ndla.imageapi.ImageApiProperties.{DefaultPageSize, MaxPageSize}
import no.ndla.imageapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.imageapi.model.api
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.TestData.searchSettings
import no.ndla.imageapi.{ImageApiProperties, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.{CC_BY_NC_SA, PublicDomain}
import no.ndla.network.ApplicationUrl
import no.ndla.scalatestsuite.IntegrationSuite
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{Outcome, PrivateMethodTester}

import scala.util.Success

class ImageSearchServiceTest
    extends IntegrationSuite(EnableElasticsearchContainer = true)
    with UnitSuite
    with TestEnvironment
    with PrivateMethodTester {

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    assume(elasticSearchContainer.isSuccess)
    super.withFixture(test)
  }

  override val e4sClient: NdlaE4sClient =
    Elastic4sClientFactory.getClient(elasticSearchHost.getOrElse("http://localhost:9200"))

  override val searchConverterService = new SearchConverterService
  override val converterService = new ConverterService
  override val imageIndexService = new ImageIndexService
  override val imageSearchService = new ImageSearchService

  val getStartAtAndNumResults: PrivateMethod[(Int, Int)] = PrivateMethod[(Int, Int)](Symbol("getStartAtAndNumResults"))

  val largeImage = Image("large-full-url", 10000, "jpg")
  val smallImage = Image("small-full-url", 100, "jpg")

  val byNcSa = Copyright(CC_BY_NC_SA.toString,
                         "Gotham City",
                         List(Author("Forfatter", "DC Comics")),
                         List(),
                         List(),
                         None,
                         None,
                         None)

  val publicDomain = Copyright(PublicDomain.toString,
                               "Metropolis",
                               List(Author("Forfatter", "Bruce Wayne")),
                               List(),
                               List(),
                               None,
                               None,
                               None)
  val updated: Date = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val agreement1Copyright = api.Copyright(api.License("gnu", "gnustuff", Some("http://gnugnusen")),
                                          "Simsalabim",
                                          List(),
                                          List(),
                                          List(),
                                          None,
                                          None,
                                          None)

  val image1 = ImageMetaInformation(
    Some(1),
    List(ImageTitle("Batmen er på vift med en bil", "nb")),
    List(ImageAltText("Bilde av en bil flaggermusmann som vifter med vingene bil.", "nb")),
    largeImage.fileName,
    largeImage.size,
    largeImage.contentType,
    byNcSa,
    List(ImageTag(List("fugl"), "nb")),
    List(),
    "ndla124",
    updated,
    updated,
    "ndla124",
    ModelReleasedStatus.YES,
    Seq.empty
  )

  val image2 = ImageMetaInformation(
    Some(2),
    List(ImageTitle("Pingvinen er ute og går", "nb")),
    List(ImageAltText("Bilde av en en pingvin som vagger borover en gate.", "nb")),
    largeImage.fileName,
    largeImage.size,
    largeImage.contentType,
    publicDomain,
    List(ImageTag(List("fugl"), "nb")),
    List(),
    "ndla124",
    updated,
    updated,
    "ndla124",
    ModelReleasedStatus.YES,
    Seq.empty
  )

  val image3 = ImageMetaInformation(
    Some(3),
    List(ImageTitle("Donald Duck kjører bil", "nb")),
    List(ImageAltText("Bilde av en en and som kjører en rød bil.", "nb")),
    smallImage.fileName,
    smallImage.size,
    smallImage.contentType,
    byNcSa,
    List(ImageTag(List("and"), "nb")),
    List(),
    "ndla124",
    updated,
    updated,
    "ndla124",
    ModelReleasedStatus.YES,
    Seq.empty
  )

  val image4 = ImageMetaInformation(
    Some(4),
    List(ImageTitle("Hulken er ute og lukter på blomstene", "unknown")),
    Seq(),
    smallImage.fileName,
    smallImage.size,
    smallImage.contentType,
    byNcSa,
    Seq(),
    Seq(),
    "ndla124",
    updated,
    updated,
    "ndla124",
    ModelReleasedStatus.YES,
    Seq.empty
  )

  val image5 = ImageMetaInformation(
    Some(5),
    List(ImageTitle("Dette er et urelatert bilde", "unknown"),
         ImageTitle("This is a unrelated photo", "en"),
         ImageTitle("Nynoreg", "nn")),
    Seq(ImageAltText("urelatert alttext", "unknown"), ImageAltText("Nynoreg", "nn")),
    smallImage.fileName,
    smallImage.size,
    smallImage.contentType,
    byNcSa.copy(agreementId = Some(1)),
    Seq(),
    Seq(),
    "ndla124",
    updated,
    updated,
    "ndla124",
    ModelReleasedStatus.YES,
    Seq.empty
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (elasticSearchContainer.isSuccess) {
      val indexName = imageIndexService.createIndexWithGeneratedName
      val alias = imageIndexService.updateAliasTarget(None, indexName.get)

      when(draftApiClient.getAgreementCopyright(any[Long])).thenReturn(None)

      when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(agreement1Copyright))

      imageIndexService.indexDocument(image1)
      imageIndexService.indexDocument(image2)
      imageIndexService.indexDocument(image3)
      imageIndexService.indexDocument(image4)
      imageIndexService.indexDocument(image5)

      val servletRequest = mock[HttpServletRequest]
      when(servletRequest.getHeader(any[String])).thenReturn("http")
      when(servletRequest.getServerName).thenReturn("localhost")
      when(servletRequest.getServletPath).thenReturn("/image-api/v2/images/")
      ApplicationUrl.set(servletRequest)

      blockUntil(() => imageSearchService.countDocuments() == 5)
    }
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    imageSearchService invokePrivate getStartAtAndNumResults(None, None) should equal((0, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    imageSearchService invokePrivate getStartAtAndNumResults(None, Some(10001)) should equal((0, MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    imageSearchService invokePrivate getStartAtAndNumResults(Some(page), None) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val pageSize = 43
    val expectedStartAt = (page - 1) * pageSize
    imageSearchService invokePrivate getStartAtAndNumResults(Some(page), Some(pageSize)) should equal(
      (expectedStartAt, pageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy())
    searchResult.totalCount should be(5)
    searchResult.results.size should be(5)
    searchResult.page.get should be(1)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("5")
  }

  test("That all filtering on minimumsize only returns images larger than minimumsize") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy(minimumSize = Some(500)))
    searchResult.totalCount should be(2)
    searchResult.results.size should be(2)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("2")
  }

  test("That all filtering on license only returns images with given license") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(license = Some(PublicDomain.toString)))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val Success(searchResultPage1) =
      imageSearchService.matchingQuery(searchSettings.copy(page = Some(1), pageSize = Some(2)))
    val Success(searchResultPage2) =
      imageSearchService.matchingQuery(searchSettings.copy(page = Some(2), pageSize = Some(2)))
    searchResultPage1.totalCount should be(5)
    searchResultPage1.page.get should be(1)
    searchResultPage1.pageSize should be(2)
    searchResultPage1.results.size should be(2)
    searchResultPage1.results.head.id should be("1")
    searchResultPage1.results.last.id should be("2")

    searchResultPage2.totalCount should be(5)
    searchResultPage2.page.get should be(2)
    searchResultPage2.pageSize should be(2)
    searchResultPage2.results.size should be(2)
    searchResultPage2.results.head.id should be("3")
    searchResultPage2.results.last.id should be("4")
  }

  test("That both minimum-size and license filters are applied.") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(
        searchSettings.copy(minimumSize = Some(500), license = Some(PublicDomain.toString)))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches title and alttext ordered by relevance") {
    val res = imageSearchService.matchingQuery(searchSettings.copy(query = Some("bil")))
    val Success(searchResult) = res
    searchResult.totalCount should be(2)
    searchResult.results.size should be(2)
    searchResult.results.head.id should be("1")
    searchResult.results.last.id should be("3")
  }

  test("That search matches title") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("Pingvinen"), language = Some("nb")))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches id search") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("1"), language = Some("nb")))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("1")
  }

  test("That search on author matches corresponding author on image") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy(query = Some("Bruce Wayne")))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("2")
  }

  test("That search matches tags") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("and"), language = Some("nb")))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("3")
  }

  test("That search defaults to nb if no language is specified") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy(query = Some("Bilde av en and")))
    searchResult.totalCount should be(4)
    searchResult.results.size should be(4)
    searchResult.results.head.id should be("1")
    searchResult.results(1).id should be("2")
    searchResult.results(2).id should be("3")
    searchResult.results.last.id should be("5")
  }

  test("That search matches title with unknown language analyzed in Norwegian") {
    val Success(searchResult) = imageSearchService.matchingQuery(searchSettings.copy(query = Some("blomst")))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("4")
  }

  test("Searching with logical AND only returns results with all terms") {
    val Success(search1) =
      imageSearchService.matchingQuery(
        searchSettings.copy(
          query = Some("batmen AND bil"),
          language = Some("nb"),
          page = Some(1),
          pageSize = Some(10)
        ))
    search1.results.map(_.id) should equal(Seq("1", "3"))

    val Success(search2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("batmen | pingvinen"),
        language = Some("nb"),
        page = Some(1),
        pageSize = Some(10)
      ))
    search2.results.map(_.id) should equal(Seq("1", "2"))

    val Success(search3) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("bilde + -flaggermusmann"),
        language = Some("nb"),
        page = Some(1),
        pageSize = Some(10)
      ))
    search3.results.map(_.id) should equal(Seq("2", "3"))

    val Success(search4) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("batmen + bil"),
        language = Some("nb"),
        page = Some(1),
        pageSize = Some(10)
      ))
    search4.results.map(_.id) should equal(Seq("1"))
  }

  test("Agreement information should be used in search") {
    val Success(searchResult) =
      imageSearchService.matchingQuery(searchSettings.copy(query = Some("urelatert")))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("5")
    searchResult.results.head.license should equal(agreement1Copyright.license.license)
  }

  test("Searching for multiple languages should returned matched language") {
    val Success(searchResult1) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("urelatert"),
        language = Some("all")
      ))
    searchResult1.totalCount should be(1)
    searchResult1.results.size should be(1)
    searchResult1.results.head.id should be("5")
    searchResult1.results.head.title.language should equal("unknown")
    searchResult1.results.head.altText.language should equal("unknown")

    val Success(searchResult2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("unrelated"),
        language = Some("all"),
        sort = Sort.ByTitleDesc
      ))
    searchResult2.totalCount should be(1)
    searchResult2.results.size should be(1)
    searchResult2.results.head.id should be("5")
    searchResult2.results.head.title.language should equal("en")
    searchResult2.results.head.altText.language should equal("unknown")
  }

  test("That field should be returned in another language if match does not contain searchLanguage") {
    val Success(searchResult) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("unrelated"),
        language = Some("en"),
      ))
    searchResult.totalCount should be(1)
    searchResult.results.size should be(1)
    searchResult.results.head.id should be("5")
    searchResult.results.head.title.language should equal("en")
    searchResult.results.head.altText.language should equal("unknown")

    val Success(searchResult2) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("nynoreg"),
        language = Some("nn"),
      ))
    searchResult2.totalCount should be(1)
    searchResult2.results.size should be(1)
    searchResult2.results.head.id should be("5")
    searchResult2.results.head.title.language should equal("nn")
    searchResult2.results.head.altText.language should equal("nn")
  }

  test("That supportedLanguages returns in order") {
    val Success(result) = imageSearchService.matchingQuery(
      searchSettings.copy(
        query = Some("nynoreg"),
        language = Some("nn"),
      ))
    result.totalCount should be(1)
    result.results.size should be(1)

    result.results.head.supportedLanguages should be(Seq("unknown", "nn", "en"))
  }

  test("That scrolling works as expected") {
    val pageSize = 2
    val expectedIds = List("1", "2", "3", "4", "5").sliding(pageSize, pageSize).toList

    val Success(initialSearch) = imageSearchService.matchingQuery(
      searchSettings.copy(
        pageSize = Some(pageSize),
        shouldScroll = true
      ))

    val Success(scroll1) = imageSearchService.scroll(initialSearch.scrollId.get, "all")
    val Success(scroll2) = imageSearchService.scroll(scroll1.scrollId.get, "all")
    val Success(scroll3) = imageSearchService.scroll(scroll2.scrollId.get, "all")

    initialSearch.results.map(_.id) should be(expectedIds.head)
    scroll1.results.map(_.id) should be(expectedIds(1))
    scroll2.results.map(_.id) should be(expectedIds(2))
    scroll3.results.map(_.id) should be(List.empty)
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
}
