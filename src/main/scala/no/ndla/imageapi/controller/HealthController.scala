/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ImageApiProperties
import org.scalatra.{InternalServerError, Ok, ScalatraServlet}

import scalaj.http.{Http, HttpOptions}
import org.json4s._
import org.json4s.native.JsonParser

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    get("/") {
      implicit val formats = DefaultFormats

      val searchResponse = Http(s"http://0.0.0.0:${ImageApiProperties.ApplicationPort}${ImageApiProperties.ImageApiBasePath}/v2/images/").asString
      searchResponse.code match {
        case 200 =>
        case _ => InternalServerError()
      }

      val searchJson = JsonParser.parse(searchResponse.body).extract[no.ndla.imageapi.model.api.SearchResult]
      val imageUrl = searchJson.results.head.metaUrl

      if(searchJson.totalCount == 0)
        Ok()

      Http(imageUrl).asString.code match {
        case 200 => Ok()
        case _ => InternalServerError()
      }
    }
  }
}
