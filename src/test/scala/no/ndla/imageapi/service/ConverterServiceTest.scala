/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.{ImageApiProperties, TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl
import org.mockito.Mockito._

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService

  val full = Image("123.png", 200, "image/png")
  val DefaultImageMetaInformation = ImageMetaInformation(Some(1), List(), List(), full.url, full.size, full.contentType, Copyright(License("", "", None), "", List()), List(), List())

  override def beforeEach = {
    val request = mock[HttpServletRequest]
    when(request.getServerPort).thenReturn(80)
    when(request.getScheme).thenReturn("http")
    when(request.getServerName).thenReturn("image-api")
    when(request.getServletPath).thenReturn("/v1/images")

    ApplicationUrl.set(request)
  }

  override def afterEach = {
    ApplicationUrl.clear()
  }

  test("That asApiImageMetaInformationWithApplicationUrl returns links with applicationUrl") {
    val api = converterService.asApiImageMetaInformationWithApplicationUrl(DefaultImageMetaInformation)
    api.metaUrl should equal ("http://image-api/v1/images/1")
    api.imageUrl should equal ("http://image-api/v1/raw/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrl returns links with domain urls") {
    val api = converterService.asApiImageMetaInformationWithDomainUrl(DefaultImageMetaInformation)
    api.metaUrl should equal (s"${ImageApiProperties.ImageApiUrlBase}1")
    api.imageUrl should equal (s"${ImageApiProperties.RawImageUrlBase}123.png")
  }

}
