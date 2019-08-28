/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import javax.servlet.http.HttpServletRequest
import no.ndla.imageapi.model.api
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl
import org.json4s.DefaultFormats
import org.json4s.native.JsonParser
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService = new ReadService
  override val converterService = new ConverterService

  override def beforeEach = {
    val applicationUrl = mock[HttpServletRequest]
    when(applicationUrl.getServerPort).thenReturn(80)
    when(applicationUrl.getHeader(any[String])).thenReturn(null)
    when(applicationUrl.getScheme).thenReturn("http")
    when(applicationUrl.getServerName).thenReturn("test.test")
    when(applicationUrl.getServletPath).thenReturn("/image-api/v2/images")
    ApplicationUrl.set(applicationUrl)
  }

  test("That path to id conversion works as expected for id paths") {

    ???
    // TODO:
//    readService.getIdFromPath("/image-api/raw/id/1234").get.id should be("1234")
//    readService.getIdFromPath("/image-api/raw/id/1234").get.id should be("1234")
  }

  test("That withId returns with agreement license and authors") {
    when(draftApiClient.getAgreementCopyright(1)).thenReturn(
      Some(api.Copyright(
        api.License("gnu", "gnuggert", Some("https://gnuli/")),
        "http://www.scanpix.no",
        List(api.Author("Forfatter", "Knutulf Knagsen")),
        List(),
        List(),
        None,
        None,
        None
      )))
    implicit val formats: DefaultFormats.type = DefaultFormats
    val testUrl = "http://test.test/image-api/v2/images/1"
    val testRawUrl = "http://test.test/image-api/raw/Elg.jpg"
    val expectedBody =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testRawUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId": 1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expectedObject = JsonParser.parse(expectedBody).extract[api.ImageMetaInformationV2]
    val agreementElg = domain.ImageMetaInformation(
      Some(1),
      List(domain.ImageTitle("Elg i busk", "nb")),
      List(domain.ImageAltText("Elg i busk", "nb")),
      "Elg.jpg",
      2865539,
      "image/jpeg",
      domain.Copyright(
        TestData.ByNcSa,
        "http://www.scanpix.no",
        List(domain.Author("Fotograf", "Test Testesen")),
        List(domain.Author("Redaksjonelt", "Kåre Knegg")),
        List(domain.Author("Leverandør", "Leverans Leveransensen")),
        Some(1),
        None,
        None
      ),
      List(domain.ImageTag(List("rovdyr", "elg"), "nb")),
      List(domain.ImageCaption("Elg i busk", "nb")),
      "ndla124",
      TestData.updated()
    )

    when(imageRepository.withId(1)).thenReturn(Some(agreementElg))
    val result = readService.withId(1, None)
    result should be(Some(expectedObject))
  }

  test("That GET /<id> returns body with original copyright if agreement doesnt exist") {
    when(draftApiClient.getAgreementCopyright(1)).thenReturn(None)
    implicit val formats: DefaultFormats.type = DefaultFormats
    val testUrl = "http://test.test/image-api/v2/images/1"
    val testRawUrl = "http://test.test/image-api/raw/Elg.jpg"
    val expectedBody =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testRawUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"CC-BY-NC-SA-4.0","description":"Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International","url":"https://creativecommons.org/licenses/by-nc-sa/4.0/"}, "agreementId":1, "origin":"http://www.scanpix.no","creators":[{"type":"Fotograf","name":"Test Testesen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[{"type":"Leverandør","name":"Leverans Leveransensen"}]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expectedObject = JsonParser.parse(expectedBody).extract[api.ImageMetaInformationV2]
    val agreementElg = domain.ImageMetaInformation(
      Some(1),
      List(domain.ImageTitle("Elg i busk", "nb")),
      List(domain.ImageAltText("Elg i busk", "nb")),
      "Elg.jpg",
      2865539,
      "image/jpeg",
      domain.Copyright(
        TestData.ByNcSa,
        "http://www.scanpix.no",
        List(domain.Author("Fotograf", "Test Testesen")),
        List(domain.Author("Redaksjonelt", "Kåre Knegg")),
        List(domain.Author("Leverandør", "Leverans Leveransensen")),
        Some(1),
        None,
        None
      ),
      List(domain.ImageTag(List("rovdyr", "elg"), "nb")),
      List(domain.ImageCaption("Elg i busk", "nb")),
      "ndla124",
      TestData.updated()
    )

    when(imageRepository.withId(1)).thenReturn(Some(agreementElg))
    readService.withId(1, None) should be(Some(expectedObject))
  }

}
