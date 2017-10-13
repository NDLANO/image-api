/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito.when
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatra.{InternalServerError, Ok}

import scalaj.http.HttpResponse

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new HealthController

  val imageSearchBody = s"""{
                           |  "totalCount": 1,
                           |  "page": 1,
                           |  "pageSize": 10,
                           |  "results": [
                           |    {
                           |      "id": "1",
                           |      "title": {
                           |        "title": "Meterolog Kristian Trægde i 1986",
                           |        "language": "unknown"
                           |      },
                           |      "altText": {
                           |        "alttext": "",
                           |        "language": "nb"
                           |      },
                           |      "previewUrl": "http://0.0.0.0/image-api/raw/sx873733_1.jpg",
                           |      "metaUrl": "http://0.0.0.0/image-api/v2/images/1",
                           |      "license": "by-nc-sa"
                           |    }
                           |  ]
                           |}""".stripMargin

  test("that getReturnCode returns 200 when response is okay") {
    val searchResponse = mock[HttpResponse[String]]
    when(searchResponse.code).thenReturn(200)
    when(searchResponse.body).thenReturn(imageSearchBody)
    val imageResponse = controller.getReturnCode(searchResponse)

    imageResponse should equal(Ok())
  }

  test("that getReturnCode returns InternalServerError when response is incorrect") {
    val imageNotFoundBody = """{
                          |	"code": "NOT FOUND",
                          |	"description": "Image with id 3 and language None not found",
                          |	"occuredAt": "2017-10-13 08:48:58.999"
                          |}""".stripMargin

    val searchResponse = mock[HttpResponse[String]]
    when(searchResponse.code).thenReturn(404)
    when(searchResponse.body).thenReturn(imageNotFoundBody)
    val imageResponse = controller.getReturnCode(searchResponse)

    imageResponse should equal(InternalServerError())
  }

  test("that url is fetched properly") {
    val searchResponse = mock[HttpResponse[String]]
    when(searchResponse.code).thenReturn(200)
    when(searchResponse.body).thenReturn(imageSearchBody)
    val expectedUrl = "http://0.0.0.0/image-api/v2/images/1"
    val (url, totalCount) = controller.getImageUrl(searchResponse)

    url should equal(expectedUrl)
    totalCount should equal(1)
  }
}
