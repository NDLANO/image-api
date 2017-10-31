/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.api.{ImageMetaSummary, SearchResult}
import org.json4s._
import org.json4s.native.JsonParser
import org.scalatra._

import scalaj.http.{Http, HttpResponse}
import com.netaporter.uri.dsl._

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    implicit val formats = DefaultFormats

    def getApiResponse(url: String): HttpResponse[String] = {
      Http(url).execute()
    }

    def getImageUrl(body: String): (Option[String], Long) = {
      def localInstanceImageUrl(cloudfrontUrl: String) = {
        s"http://0.0.0.0${ImageApiProperties.ImageApiBasePath}/raw/${cloudfrontUrl.pathParts.last.part}"
      }
      val json = JsonParser.parse(body).extract[SearchResult]
      json.results.headOption match {
        case Some(result: ImageMetaSummary) => (Some(localInstanceImageUrl(result.previewUrl)), json.totalCount)
        case _ => (None,json.totalCount)
      }
    }

    def getReturnCode(imageResponse: HttpResponse[String]): ActionResult = {
      imageResponse.code match {
        case 200 => Ok()
        case _ => InternalServerError()
      }
    }

    get("/") {
      val apiSearchResponse = getApiResponse(
        s"http://0.0.0.0:${ImageApiProperties.ApplicationPort}${ImageApiProperties.ImageApiBasePath}/v2/images/")
      val (imageUrl,totalCount) = getImageUrl(apiSearchResponse.body)

      apiSearchResponse.code match {
        case _ if totalCount == 0 => Ok()
        case 200 => getReturnCode(getApiResponse(imageUrl.get))
        case _ => InternalServerError()
      }
    }
  }
}
