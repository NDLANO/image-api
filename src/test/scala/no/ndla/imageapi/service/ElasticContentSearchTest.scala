package no.ndla.imageapi.service

import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class ElasticContentSearchTest extends UnitSuite with TestEnvironment {
  var service: ElasticContentSearch = _
  val getStartAtAndNumResults = PrivateMethod[(Int, Int)]('getStartAtAndNumResults)

  override def beforeEach() = {
    service = new ElasticContentSearch
  }

  "getStartAtAndNumResults" should "return default values for None-input" in {
    service invokePrivate getStartAtAndNumResults(None, None) should equal((0, DEFAULT_PAGE_SIZE))
  }

  it should "return SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE" in {
    service invokePrivate getStartAtAndNumResults(None, Some(1000)) should equal((0, MAX_PAGE_SIZE))
  }

  it should "return the correct calculated start at for page and page-size with default page-size" in {
    val page = 74
    val expectedStartAt = (page - 1) * DEFAULT_PAGE_SIZE
    service invokePrivate getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DEFAULT_PAGE_SIZE))
  }

  it should "return the correct calculated start at for page and page-size" in {
    val page = 123
    val pageSize = 321
    val expectedStartAt = (page - 1) * pageSize
    service invokePrivate getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }
}
