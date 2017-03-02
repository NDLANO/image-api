/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, NativeSwaggerBase, Swagger}

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object ImagesApiInfo {
  val apiInfo = ApiInfo(
  "Images Api",
  "Documentation for the Images API of NDLA.no",
  "https://ndla.no",
  ImageApiProperties.ContactEmail,
  "GPL v3.0",
  "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class ImageSwagger extends Swagger(Swagger.SpecVersion, "0.8", ImagesApiInfo.apiInfo)
