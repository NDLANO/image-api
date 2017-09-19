/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.model.{S3UploadException, api, domain}
import no.ndla.imageapi.{ImageApiProperties, TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.jackson.Serialization._
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  lazy val controller = new InternController
  addServlet(controller, "/*")
  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val DefaultApiImageMetaInformation = api.ImageMetaInformation("1", s"${ImageApiProperties.ImageApiUrlBase}1", List(), List(), s"${ImageApiProperties.RawImageUrlBase}/test.jpg", 0, "", api.Copyright(api.License("", "", None), "", List()), List(), List())
  val DefaultDomainImageMetaInformation = domain.ImageMetaInformation(Some(1), List(), List(), "test.jpg", 0, "", domain.Copyright(domain.License("", "", None), "", List()), List(), List(), "ndla124", updated)

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

  test("That POST /import/123 returns 504 with error message when import failed on S3 upload") {
    when(importService.importImage(eqTo("123"))).thenThrow(new S3UploadException(s"Upload of image:[name] to S3 failed."))
    post("/import/123") {
      status should equal (504)
      body indexOf "Upload of image:[name] to S3 failed" should be > 0
    }
  }

}
