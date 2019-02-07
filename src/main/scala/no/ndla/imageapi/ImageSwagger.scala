/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object ImagesApiInfo {

  val apiInfo = ApiInfo(
    "Image API",
    "Searching and fetching all images used in the NDLA platform.\n\n" +
      "",
    "https://om.ndla.no/terms",
    ImageApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )
}

class ImageSwagger extends Swagger("2.0", "1.0", ImagesApiInfo.apiInfo) {
  addAuthorization(
    OAuth(List(ImageApiProperties.RoleWithWriteAccess),
          List(ImplicitGrant(LoginEndpoint(ImageApiProperties.Auth0LoginEndpoint), "access_token"))))
}
