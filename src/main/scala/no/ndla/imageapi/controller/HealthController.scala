/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import org.scalatra.{NoContent, ScalatraServlet}

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    get("/") {
      NoContent()
    }
  }
}
