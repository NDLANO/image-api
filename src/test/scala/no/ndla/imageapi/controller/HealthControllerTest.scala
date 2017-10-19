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

import scalaj.http.HttpResponse

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {


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
  val imageResultBody = """{
                          |	"id": "1",
                          |	"metaUrl": "http://0.0.0.0/image-api/v2/images/1",
                          |	"title": {
                          |		"title": "Meterolog Kristian Trægde i 1986",
                          |		"language": "unknown"
                          |	},
                          |	"alttext": {
                          |		"alttext": "",
                          |		"language": "nb"
                          |	},
                          |	"imageUrl": "http://0.0.0.0/image-api/raw/sx873733_1.jpg",
                          |	"size": 238472,
                          |	"contentType": "image/jpeg",
                          |	"copyright": {
                          |		"license": {
                          |			"license": "by-nc-sa",
                          |			"description": "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic",
                          |			"url": "https://creativecommons.org/licenses/by-nc-sa/2.0/"
                          |		},
                          |		"origin": "http://www.scanpix.no",
                          |		"authors": [
                          |			{
                          |				"type": "Fotograf",
                          |				"name": "Rolf M Aagaard"
                          |			},
                          |			{
                          |				"type": "Leverandør",
                          |				"name": "Aftenposten"
                          |			},
                          |			{
                          |				"type": "Leverandør",
                          |				"name": "NTB scanpix"
                          |			}
                          |		]
                          |	},
                          |	"tags": {
                          |		"tags": [
                          |			"kristian trægde",
                          |			"mediepåvirkning",
                          |			"meterolog"
                          |		],
                          |		"language": "nb"
                          |	},
                          |	"caption": {
                          |		"caption": "",
                          |		"language": "nb"
                          |	},
                          |	"supportedLanguages": [
                          |		"nn",
                          |		"nb",
                          |		"unknown",
                          |		"en"
                          |	]
                          |}""".stripMargin
  val httpResponseMock: HttpResponse[String] = mock[HttpResponse[String]]

  lazy val controller = new HealthController {
    override def getApiResponse(url: String): HttpResponse[String] = httpResponseMock
  }
  addServlet(controller, "/")

  test("that url is fetched properly") {
    val expectedUrl = "http://0.0.0.0/image-api/v2/images/1"
    val (url, totalCount) = controller.getImageUrl(imageSearchBody)

    url should equal(Some(expectedUrl))
    totalCount should equal(1)
  }

  test("that /health returns 200 on success") {
    when(httpResponseMock.code).thenReturn(200)
    when(httpResponseMock.body).thenReturn(imageSearchBody).thenReturn(imageResultBody)

    get("/") {
      status should equal (200)
    }
  }

  test("that /health returns 500 on failure") {
    when(httpResponseMock.code).thenReturn(500)
    when(httpResponseMock.body).thenReturn(imageSearchBody).thenReturn(imageResultBody)

    get("/") {
      status should equal(500)
    }
  }

  test("that /health returns 200 on no images") {
    val noImageBody = s"""{
                         |	"totalCount": 0,
                         |	"page": 1,
                         |	"pageSize": 10,
                         | "results": []
                         |}""".stripMargin
    val notFoundBody = s"""{
                          |	"code": "NOT FOUND",
                          |	"description": "Image with id 1131123 and language Some(nb) not found",
                          |	"occurredAt": "2017-10-13 12:35:33.801"
                          |}""".stripMargin
    when(httpResponseMock.code).thenReturn(200).thenReturn(404)
    when(httpResponseMock.body).thenReturn(noImageBody).thenReturn(notFoundBody)

    get("/") {
      status should equal(200)
    }
  }
}
