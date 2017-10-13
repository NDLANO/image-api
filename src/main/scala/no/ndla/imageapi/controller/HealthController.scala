/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.api.SearchResult
import org.json4s._
import org.json4s.native.JsonParser
import org.scalatra.{ActionResult, InternalServerError, Ok, ScalatraServlet}

import scalaj.http.{Http, HttpResponse}

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    implicit val formats = DefaultFormats

    def getImageSearchResponse(url: String): HttpResponse[String] = {
      Http(url).asString
    }

    def getImageUrl(response: HttpResponse[String]): (String, Long) = {
      val json = JsonParser.parse(response.body).extract[SearchResult]
      (json.results.head.metaUrl,json.totalCount)
    }

    def getReturnCode(imageResponse: HttpResponse[String]): ActionResult = {
      imageResponse.code match {
        case 200 => Ok()
        case _ => InternalServerError()
      }
    }

    get("/") {
      val searchResponse = getImageSearchResponse(
        s"http://0.0.0.0:${ImageApiProperties.ApplicationPort}${ImageApiProperties.ImageApiBasePath}/v2/images/")
      val (imageUrl,totalCount) = getImageUrl(searchResponse)

      if(totalCount == 0)
        Ok()

      searchResponse.code match {
        case 200 => getReturnCode(Http(imageUrl).asString)
        case _ => InternalServerError()
      }
    }
  }
}
