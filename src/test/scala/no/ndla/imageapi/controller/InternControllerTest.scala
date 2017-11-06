/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.model.api.{ImageAltText, ImageCaption, ImageTag, ImageTitle}
import no.ndla.imageapi.model.{S3UploadException, api, domain}
import no.ndla.imageapi.{ImageApiProperties, TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.jackson.Serialization._
import org.mockito.Matchers
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  override val converterService = new ConverterService
  lazy val controller = new InternController
  addServlet(controller, "/*")
  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val DefaultApiImageMetaInformation = api.ImageMetaInformationV2(
    "1",
    s"${ImageApiProperties.ImageApiUrlBase}1",
    ImageTitle("", "nb"),
    ImageAltText("", "nb"),
    s"${ImageApiProperties.RawImageUrlBase}/test.jpg",
    0,
    "",
    api.Copyright(api.License("", "", None), "", List(), List(), List(), None, None),
    ImageTag(Seq.empty, "nb"),
    ImageCaption("","nb"),
    Seq())

  val DefaultDomainImageMetaInformation = domain.ImageMetaInformation(Some(1), List(), List(), "test.jpg", 0, "", domain.Copyright(domain.License("", "", None), "", List(), None, None), List(), List(), "ndla124", updated)

  override def beforeEach = {
    reset(imageRepository, importService, indexService, indexBuilderService)
  }

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

  test("That DELETE /index removes all indexes") {
    when(indexService.findAllIndexes()).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success("")).when(indexService).deleteIndex(Some("index1"))
    doReturn(Success("")).when(indexService).deleteIndex(Some("index2"))
    doReturn(Success("")).when(indexService).deleteIndex(Some("index3"))
    delete("/index") {
      status should equal (200)
      body should equal ("Deleted 3 indexes")
    }
    verify(indexService).findAllIndexes()
    verify(indexService).deleteIndex(Some("index1"))
    verify(indexService).deleteIndex(Some("index2"))
    verify(indexService).deleteIndex(Some("index3"))
    verifyNoMoreInteractions(indexService)
  }

  test("That DELETE /index fails if at least one index isn't found, and no indexes are deleted") {
    doReturn(Failure(new RuntimeException("Failed to find indexes"))).when(indexService).findAllIndexes()
    doReturn(Success("")).when(indexService).deleteIndex(Some("index1"))
    doReturn(Success("")).when(indexService).deleteIndex(Some("index2"))
    doReturn(Success("")).when(indexService).deleteIndex(Some("index3"))
    delete("/index") {
      status should equal (500)
      body should equal ("Failed to find indexes")
    }
    verify(indexService, never()).deleteIndex(Matchers.anyObject())
  }

  test("That DELETE /index fails if at least one index couldn't be deleted, but the other indexes are deleted regardless") {
    when(indexService.findAllIndexes()).thenReturn(Success(List("index1", "index2", "index3")))
    doReturn(Success("")).when(indexService).deleteIndex(Some("index1"))
    doReturn(Failure(new RuntimeException("No index with name 'index2' exists"))).when(indexService).deleteIndex(Some("index2"))
    doReturn(Success("")).when(indexService).deleteIndex(Some("index3"))
    delete("/index") {
      status should equal (500)
      body should equal ("Failed to delete 1 index: No index with name 'index2' exists. 2 indexes were deleted successfully.")
    }
    verify(indexService).deleteIndex(Some("index1"))
    verify(indexService).deleteIndex(Some("index2"))
    verify(indexService).deleteIndex(Some("index3"))
  }

}
