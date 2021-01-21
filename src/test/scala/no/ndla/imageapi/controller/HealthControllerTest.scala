/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito.when
import org.scalatra.test.scalatest.ScalatraFunSuite

import scalaj.http.HttpResponse

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  val httpResponseMock: HttpResponse[String] = mock[HttpResponse[String]]

  lazy val controller = new HealthController {
    override def getApiResponse(url: String): HttpResponse[String] = httpResponseMock
  }
  addServlet(controller, "/")

  test("that /health returns 200 on success") {
    when(httpResponseMock.code).thenReturn(200)
    when(imageRepository.getRandomImage()).thenReturn(Some(TestData.bjorn))

    get("/") {
      status should equal(200)
    }
  }

  test("that /health returns 500 on failure") {
    when(httpResponseMock.code).thenReturn(500)
    when(imageRepository.getRandomImage()).thenReturn(Some(TestData.elg))

    get("/") {
      status should equal(500)
    }
  }

  test("that /health returns 200 on no images") {
    when(httpResponseMock.code).thenReturn(404)
    when(imageRepository.getRandomImage()).thenReturn(None)

    get("/") {
      status should equal(200)
    }
  }
}
