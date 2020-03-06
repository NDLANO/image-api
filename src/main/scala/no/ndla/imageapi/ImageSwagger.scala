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

  val contactInfo = ContactInfo(
    "NDLA",
    "ndla.no",
    ImageApiProperties.ContactEmail
  )

  val licenseInfo = LicenseInfo(
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )

  val apiInfo = ApiInfo(
    "Image API",
    "Searching and fetching all images used in the NDLA platform.\n\n" +
      "The Image API provides an endpoint for searching in and fetching images used in NDLA resources. Meta-data are " +
      "also searched and returned in the results. Examples of meta-data are title, alt-text, language and license.\n" +
      "The API can resize and crop transitions on the returned images to enable use in special contexts, e.g. " +
      "low bandwidth scenarios",
    "https://om.ndla.no/tos",
    contactInfo,
    licenseInfo
  )
}

class ImageSwagger extends Swagger("2.0", "1.0", ImagesApiInfo.apiInfo) {
  addAuthorization(
    OAuth(List(ImageApiProperties.RoleWithWriteAccess),
          List(ImplicitGrant(LoginEndpoint(ImageApiProperties.Auth0LoginEndpoint), "access_token"))))
}
