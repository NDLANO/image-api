package no.ndla.imageapi.service

import com.sksamuel.elastic4s.testkit.ElasticSugar
import no.ndla.imageapi.model._
import no.ndla.imageapi.{TestEnvironment, UnitSuite}


class ElasticContentSearchTest extends UnitSuite with TestEnvironment with ElasticSugar {

  override val elasticClient = client
  override val elasticContentIndex = new ElasticContentIndex
  override val searchService = new ElasticContentSearch

  val getStartAtAndNumResults = PrivateMethod[(Int, Int)]('getStartAtAndNumResults)

  val largeImageVariant = ImageVariants(Some(Image("large-thumb-url", 1000, "jpg")), Some(Image("large-full-url", 10000, "jpg")))
  val smallImageVariant = ImageVariants(Some(Image("small-thumb-url", 10, "jpg")), Some(Image("small-full-url", 100, "jpg")))

  val byNcSa = Copyright(License("by-nc-sa", "Attribution-NonCommercial-ShareAlike", None), "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright(License("publicdomain", "Public Domain", None), "Metropolis", List(Author("Forfatter", "Bruce Wayne")))

  val image1 = ImageMetaInformation("1", List(ImageTitle("Batmen er på vift med en bil", Some("nb"))), List(ImageAltText("Bilde av en bil flaggermusmann som vifter med vingene bil.", Some("nb"))), largeImageVariant, byNcSa, List(ImageTag("fugl", Some("nb"))))
  val image2 = ImageMetaInformation("2", List(ImageTitle("Pingvinen er ute og går", Some("nb"))), List(ImageAltText("Bilde av en en pingvin som vagger borover en gate.", Some("nb"))), largeImageVariant, publicDomain, List(ImageTag("fugl", Some("nb"))))
  val image3 = ImageMetaInformation("3", List(ImageTitle("Donald Duck kjører bil", Some("nb"))), List(ImageAltText("Bilde av en en and som kjører en rød bil.", Some("nb"))), smallImageVariant, byNcSa, List(ImageTag("and", Some("nb"))))

  override def beforeAll = {
    val indexName = "testindex"
    elasticContentIndex.createIndex(indexName)
    elasticContentIndex.updateAliasTarget(indexName, None)

    elasticContentIndex.indexDocuments(List(
      image1, image2, image3
    ), indexName)

    blockUntilCount(3, indexName)
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService invokePrivate getStartAtAndNumResults(None, None) should equal((0, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService invokePrivate getStartAtAndNumResults(None, Some(1000)) should equal((0, MAX_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DEFAULT_PAGE_SIZE
    searchService invokePrivate getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val pageSize = 321
    val expectedStartAt = (page - 1) * pageSize
    searchService invokePrivate getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val results = searchService.all(None, None, None, None)
    results.size should be (3)
    results.head.id should be ("1")
    results.last.id should be ("3")
  }

  test("That all filtering on minimumsize only returns images larger than minimumsize") {
    val results = searchService.all(Some(500), None, None, None)
    results.size should be (2)
    results.head.id should be ("1")
    results.last.id should be ("2")
  }

  test("That all filtering on license only returns images with given license") {
    val results = searchService.all(None, Some("publicdomain"), None, None)
    results.size should be (1)
    results.head.id should be ("2")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = searchService.all(None, None, Some(1), Some(2))
    val page2 = searchService.all(None, None, Some(2), Some(2))
    page1.size should be (2)
    page1.head.id should be ("1")
    page1.last.id should be ("2")
    page2.size should be (1)
    page2.head.id should be ("3")
  }

  test("That both minimum-size and license filters are applied.") {
    val results = searchService.all(Some(500), Some("publicdomain"), None, None)
    results.size should be (1)
    results.head.id should be ("2")
  }

  test("That search matches title and alttext ordered by relevance") {
    val results = searchService.matchingQuery(Seq("bil"), None, Some("nb"), None, None, None)
    results.size should be (2)
    results.head.id should be ("1")
    results.last.id should be ("3")
  }

  test("That search matches title") {
    val results = searchService.matchingQuery(Seq("Pingvinen"), None, Some("nb"), None, None, None)
    results.size should be (1)
    results.head.id should be ("2")
  }

  test("That search matches tags") {
    val results = searchService.matchingQuery(Seq("and"), None, Some("nb"), None, None, None)
    results.size should be (1)
    results.head.id should be ("3")
  }
}
