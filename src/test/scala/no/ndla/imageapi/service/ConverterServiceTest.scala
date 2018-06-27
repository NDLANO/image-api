/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.util.Date
import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.model.api
import no.ndla.imageapi.{ImageApiProperties, TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService

  val updated: Date = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val full = Image("/123.png", 200, "image/png")

  val DefaultImageMetaInformation = ImageMetaInformation(
    Some(1),
    List(ImageTitle("test", "nb")),
    List(),
    full.fileName,
    full.size,
    full.contentType,
    Copyright(License("", "", None), "", List(), List(), List(), None, None, None),
    List(),
    List(),
    "ndla124",
    updated
  )

  val MultiLangImage = ImageMetaInformation(
    Some(2),
    List(ImageTitle("nynorsk", "nn"), ImageTitle("english", "en"), ImageTitle("norsk", "unknown")),
    List(),
    full.fileName,
    full.size,
    full.contentType,
    Copyright(License("", "", None), "", List(), List(), List(), None, None, None),
    List(),
    List(),
    "ndla124",
    updated
  )

  override def beforeEach: Unit = {
    val request = mock[HttpServletRequest]
    when(request.getServerPort).thenReturn(80)
    when(request.getScheme).thenReturn("http")
    when(request.getServerName).thenReturn("image-api")
    when(request.getServletPath).thenReturn("/v2/images")

    ApplicationUrl.set(request)
  }

  def setApplicationUrl(): Unit = {
    val request = mock[HttpServletRequest]
    when(request.getServerPort).thenReturn(80)
    when(request.getScheme).thenReturn("http")
    when(request.getServerName).thenReturn("image-api")
    when(request.getServletPath).thenReturn("/v2/images")

    ApplicationUrl.set(request)
  }

  override def afterEach: Unit = {
    ApplicationUrl.clear()
  }

  test("That asApiImageMetaInformationWithDomainUrl returns links with domain urls") {
    val apiImage = converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, Some("nb"))
    apiImage.get.metaUrl should equal(s"${ImageApiProperties.ImageApiUrlBase}1")
    apiImage.get.imageUrl should equal(s"${ImageApiProperties.RawImageUrlBase}/123.png")
  }

  test("That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links with applicationUrl") {
    setApplicationUrl()

    val apiImage = converterService.asApiImageMetaInformationWithApplicationUrlV2(DefaultImageMetaInformation, None)
    apiImage.get.metaUrl should equal("http://image-api/v2/images/1")
    apiImage.get.imageUrl should equal("http://image-api/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links with domain urls") {
    setApplicationUrl()

    val apiImage = converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, None)
    apiImage.get.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.get.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test(
    "That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links even if language is not supported") {
    setApplicationUrl()
    val apiImage = converterService.asApiImageMetaInformationWithApplicationUrlV2(DefaultImageMetaInformation,
                                                                                  Some("RandomLangauge"))

    apiImage.get.metaUrl should equal("http://image-api/v2/images/1")
    apiImage.get.imageUrl should equal("http://image-api/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links even if language is not supported") {
    setApplicationUrl()

    val apiImage =
      converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, Some("RandomLangauge"))
    apiImage.get.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.get.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test("That asApiImageMetaInformationWithApplicationUrlV2 returns with agreement copyright features") {
    setApplicationUrl()
    val from = DateTime.now().minusDays(5).toDate()
    val to = DateTime.now().plusDays(10).toDate()
    val agreementCopyright = api.Copyright(
      api.License("gnu", "gpl", None),
      "http://tjohei.com/",
      List(),
      List(),
      List(api.Author("Supplier", "Mads LakseService")),
      None,
      Some(from),
      Some(to)
    )
    when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(agreementCopyright))
    val apiImage = converterService.asApiImageMetaInformationWithApplicationUrlV2(
      DefaultImageMetaInformation.copy(
        copyright = DefaultImageMetaInformation.copyright.copy(
          processors = List(Author("Idea", "Kaptein Snabelfant")),
          rightsholders = List(Author("Publisher", "KjeksOgKakerAS")),
          agreementId = Some(1)
        )),
      None
    )

    apiImage.get.copyright.creators.size should equal(0)
    apiImage.get.copyright.processors.head.name should equal("Kaptein Snabelfant")
    apiImage.get.copyright.rightsholders.head.name should equal("Mads LakseService")
    apiImage.get.copyright.rightsholders.size should equal(1)
    apiImage.get.copyright.license.license should equal("gnu")
    apiImage.get.copyright.validFrom.get should equal(from)
    apiImage.get.copyright.validTo.get should equal(to)
  }

  test("that asImageMetaInformationV2 properly") {
    val result1 = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None)
    result1.get.id should be("2")
    result1.get.title.language should be("unknown")

    val result2 = converterService.asImageMetaInformationV2(MultiLangImage, Some("en"), "", None)
    result2.get.id should be("2")
    result2.get.title.language should be("en")

    val result3 = converterService.asImageMetaInformationV2(MultiLangImage, Some("nn"), "", None)
    result3.get.id should be("2")
    result3.get.title.language should be("nn")

  }

  test("that asImageMetaInformationV2 returns sorted supportedLanguages") {
    val result = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None)
    result.get.supportedLanguages should be(Seq("unknown", "nn", "en"))
  }

}
