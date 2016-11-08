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
import org.mockito.Mockito._
import org.mockito.Matchers._
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.ApplicationUrl

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService

  val full = Image("full/123.png", 200, "image/png")
  val DefaultImageMetaInformation = ImageMetaInformation(Some(1), List(), List(), full.url, full.size, full.contentType, Copyright(License("", "", None), "", List()), List(), List())

  override def beforeEach = {
    val request = mock[HttpServletRequest]
    when(request.getServerPort).thenReturn(80)
    when(request.getScheme).thenReturn("http")
    when(request.getServerName).thenReturn("image-api")
    when(request.getServletPath).thenReturn("/images")

    ApplicationUrl.set(request)
  }

  override def afterEach = {
    ApplicationUrl.clear()
  }

  test("That asApiImageMetaInformationWithApplicationUrl returns links with applicationUrl") {
    val api = converterService.asApiImageMetaInformationWithApplicationUrl(DefaultImageMetaInformation)
    api.metaUrl should equal ("http://image-api/images/1")
    api.imageUrl should equal ("http://image-api/images/full/123.png")
  }

  test("That asApiImageMetaInformationWithDomainUrl returns links with domain urls") {
    val api = converterService.asApiImageMetaInformationWithDomainUrl(DefaultImageMetaInformation)
    api.metaUrl should equal ("http://somedomain/images/1")
    api.imageUrl should equal ("http://somedomain/images/full/123.png")
  }

}
