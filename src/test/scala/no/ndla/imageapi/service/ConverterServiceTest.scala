/*
 * Part of NDLA image-api.
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
import no.ndla.network.{ApplicationUrl, Domains}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService

  val updated: Date = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val full = Image("/123.png", 200, "image/png")
  val wanting = Image("123.png", 200, "image/png")

  val DefaultImageMetaInformation = ImageMetaInformation(
    Some(1),
    List(ImageTitle("test", "nb")),
    List(),
    full.fileName,
    full.size,
    full.contentType,
    Copyright("", "", List(), List(), List(), None, None, None),
    List(),
    List(),
    "ndla124",
    updated
  )

  val WantingImageMetaInformation = ImageMetaInformation(
    Some(1),
    List(ImageTitle("test", "nb")),
    List(),
    wanting.fileName,
    wanting.size,
    wanting.contentType,
    Copyright("", "", List(), List(), List(), None, None, None),
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
    Copyright("", "", List(), List(), List(), None, None, None),
    List(),
    List(),
    "ndla124",
    updated
  )

  override def beforeEach(): Unit = {
    val request = mock[HttpServletRequest]
    when(request.getServerPort).thenReturn(80)
    when(request.getScheme).thenReturn("http")
    when(request.getServerName).thenReturn("image-api")
    when(request.getServletPath).thenReturn("/v2/images")

    ApplicationUrl.set(request)
  }

  override def afterEach(): Unit = {
    ApplicationUrl.clear()
  }

  test("That asApiImageMetaInformationWithDomainUrl returns links with domain urls") {
    {
      val apiImage = converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, Some("nb"))
      apiImage.metaUrl should equal(s"${ImageApiProperties.ImageApiUrlBase}1")
      apiImage.imageUrl should equal(s"${ImageApiProperties.RawImageUrlBase}/123.png")
    }
    {
      val apiImage = converterService.asApiImageMetaInformationWithDomainUrlV2(WantingImageMetaInformation, Some("nb"))
      apiImage.metaUrl should equal(s"${ImageApiProperties.ImageApiUrlBase}1")
      apiImage.imageUrl should equal(s"${ImageApiProperties.RawImageUrlBase}/123.png")
    }
  }

  test("That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links with applicationUrl") {
    val apiImage = converterService.asApiImageMetaInformationWithApplicationUrlV2(DefaultImageMetaInformation, None)
    apiImage.metaUrl should equal(s"${ImageApiProperties.Domain}/v2/images/1")
    apiImage.imageUrl should equal(s"${ImageApiProperties.Domain}/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links with domain urls") {
    val apiImage = converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, None)
    apiImage.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test(
    "That asApiImageMetaInformationWithApplicationUrlAndSingleLanguage returns links even if language is not supported") {
    val apiImage = converterService.asApiImageMetaInformationWithApplicationUrlV2(DefaultImageMetaInformation,
                                                                                  Some("RandomLangauge"))

    apiImage.metaUrl should equal(s"${ImageApiProperties.Domain}/v2/images/1")
    apiImage.imageUrl should equal(s"${ImageApiProperties.Domain}/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrlAndSingleLanguage returns links even if language is not supported") {
    val apiImage =
      converterService.asApiImageMetaInformationWithDomainUrlV2(DefaultImageMetaInformation, Some("RandomLangauge"))
    apiImage.metaUrl should equal("http://api-gateway.ndla-local/image-api/v2/images/1")
    apiImage.imageUrl should equal("http://api-gateway.ndla-local/image-api/raw/123.png")
  }

  test("That asApiImageMetaInformationWithApplicationUrlV2 returns with agreement copyright features") {
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

    apiImage.copyright.creators.size should equal(0)
    apiImage.copyright.processors.head.name should equal("Kaptein Snabelfant")
    apiImage.copyright.rightsholders.head.name should equal("Mads LakseService")
    apiImage.copyright.rightsholders.size should equal(1)
    apiImage.copyright.license.license should equal("gnu")
    apiImage.copyright.validFrom.get should equal(from)
    apiImage.copyright.validTo.get should equal(to)
  }

  test("that asImageMetaInformationV2 properly") {
    val result1 = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None)
    result1.id should be("2")
    result1.title.language should be("unknown")

    val result2 = converterService.asImageMetaInformationV2(MultiLangImage, Some("en"), "", None)
    result2.id should be("2")
    result2.title.language should be("en")

    val result3 = converterService.asImageMetaInformationV2(MultiLangImage, Some("nn"), "", None)
    result3.id should be("2")
    result3.title.language should be("nn")

  }

  test("that asImageMetaInformationV2 returns sorted supportedLanguages") {
    val result = converterService.asImageMetaInformationV2(MultiLangImage, Some("nb"), "", None)
    result.supportedLanguages should be(Seq("unknown", "nn", "en"))
  }

  test("that withoutLanguage removes correct language") {
    val result1 = converterService.withoutLanguage(MultiLangImage, "en")
    converterService.getSupportedLanguages(result1) should be(Seq("unknown", "nn"))
    val result2 = converterService.withoutLanguage(MultiLangImage, "nn")
    converterService.getSupportedLanguages(result2) should be(Seq("unknown", "en"))
    val result3 = converterService.withoutLanguage(MultiLangImage, "unknown")
    converterService.getSupportedLanguages(result3) should be(Seq("nn", "en"))
    val result4 = converterService.withoutLanguage(converterService.withoutLanguage(MultiLangImage, "unknown"), "en")
    converterService.getSupportedLanguages(result4) should be(Seq("nn"))
  }

}
