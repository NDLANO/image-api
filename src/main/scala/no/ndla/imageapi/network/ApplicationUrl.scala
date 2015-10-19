package no.ndla.imageapi.network

import javax.servlet.http.HttpServletRequest

object ApplicationUrl {
  val HTTP_PORT = 80
  val HTTPS_PORT = 443

  val applicationUrl = new ThreadLocal[String]

  def set(request: HttpServletRequest) {
    if(request.getServerPort != HTTP_PORT && request.getServerPort != HTTPS_PORT)
      applicationUrl.set(s"${request.getScheme}://${request.getServerName}:${request.getServerPort}${request.getServletPath}/")
    else
      applicationUrl.set(s"${request.getScheme}://${request.getServerName}${request.getServletPath}/")
  }

  def get(): String = {
    applicationUrl.get
  }

  def clear(): Unit = {
    applicationUrl.remove
  }
}
