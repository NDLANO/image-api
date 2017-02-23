/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.swagger

import io.swagger.models._
import io.swagger.models.auth.OAuth2Definition
import no.ndla.imageapi.ImageApiProperties


object SwaggerInfo {
  val swagger = new Swagger()
    .info(new Info()
      .title("Images API")
      .version("v1")
      .description("Documentation for the Images API of NDLA.no")
      .termsOfService("http://ndla.no")
      .license(
        new License()
          .name("GPL v3")
          .url("http://www.gnu.org/licenses/gpl-3.0.en.html"))
      .contact(new Contact()
        .name(ImageApiProperties.ContactName)
        .email(ImageApiProperties.ContactEmail)))
    .tag(new Tag()
      .name("ImageApi-V1")
      .description("API for accessing images from ndla.no."))
    .securityDefinition(
      "imageapi_auth", new OAuth2Definition()
        .application("/auth/tokens"))
}