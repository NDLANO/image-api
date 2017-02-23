/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.swagger
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import io.swagger.util.Json
import io.swagger.util.Yaml
import io.swagger.models.Swagger

/**
  * This class can be removed when all services are listening for /swagger.json
  */
class ApiDeclarationServlet extends io.swagger.servlet.listing.ApiDeclarationServlet {
  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val swagger: Swagger = getServletContext.getAttribute("swagger").asInstanceOf[Swagger]
    if (swagger == null) {
      response.setStatus(404)
      return
    }
    val pathInfo: String = request.getPathInfo
    if ("/" == pathInfo) response.getWriter.println(Json.mapper.writeValueAsString(swagger))
    else if ("/swagger.json" == pathInfo) response.getWriter.println(Json.mapper.writeValueAsString(swagger))
    else if ("/swagger.yaml" == pathInfo) response.getWriter.println(Yaml.mapper.writeValueAsString(swagger))
    else response.setStatus(404)
  }
}
