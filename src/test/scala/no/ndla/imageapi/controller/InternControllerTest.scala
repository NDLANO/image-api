/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.model.{api, domain}
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.json4s.jackson.Serialization._
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  lazy val controller = new InternController
  addServlet(controller, "/*")

  val DefaultApiImageMetaInformation = api.ImageMetaInformation("1", "http://somedomain/images/1", List(), List(), api.ImageVariants(None, None), api.Copyright(api.License("", "", None), "", List()), List(), List())
  val DefaultDomainImageMetaInformation = domain.ImageMetaInformation(Some(1),  List(), List(), domain.ImageVariants(None, None), domain.Copyright(domain.License("", "", None), "", List()), List(), List())

  test("That GET /extern/abc returns 404") {
    when(imageRepository.withExternalId(eqTo("abc"))).thenReturn(None)
    get("/extern/abc") {
      status should equal (404)
    }
  }

  test("That GET /extern/123 returns 404 if 123 is not found") {
    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(None)
    get("/extern/123") {
      status should equal (404)
    }
  }

  test("That GET /extern/123 returns 200 and imagemeta when found") {
    implicit val formats = org.json4s.DefaultFormats

    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(Some(DefaultDomainImageMetaInformation))
    get("/extern/123") {
      status should equal (200)
      body should equal (write(DefaultApiImageMetaInformation))
    }
  }

  test("That POST /import/123 returns 200 OK when import is a success") {
    when(importService.importImage(eqTo("123"))).thenReturn(Success(DefaultDomainImageMetaInformation))
    post("/import/123") {
      status should equal (200)
    }
  }

  test("That POST /import/123 returns 500 with error message when import failed") {
    when(importService.importImage(eqTo("123"))).thenReturn(Failure(new NullPointerException("null")))
    post("/import/123") {
      status should equal (500)
      body indexOf "external_id 123 failed after" should be > 0
    }
  }

}
