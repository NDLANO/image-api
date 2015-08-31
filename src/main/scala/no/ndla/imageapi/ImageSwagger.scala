/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object ImagesApiInfo extends ApiInfo (
  "Images Api",
  "Documentation for the Images API of NDLA.no",
  "http://ndla.no",
  "kontakt-epost",
  "GPL2.0",
  "lisensurl")

class ImageSwagger extends Swagger(Swagger.SpecVersion, "0.8", ImagesApiInfo)
