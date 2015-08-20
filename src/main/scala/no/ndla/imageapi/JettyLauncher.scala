package no.ndla.imageapi

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object JettyLauncher { // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    val port = if(System.getenv("PORT") != null) System.getenv("PORT").toInt else 80

    val server = new Server(port)
    val context = new WebAppContext()

//     for debug purposes
//    val staticResources = new ResourceCollection(Array(
//      "src/main/webapp/image-api",
//      getClass.getResource("META-INF/resources/webjars").toExternalForm))

    val staticResources = new ResourceCollection(Array(
      getClass.getResource("/image-api").toExternalForm,
      getClass.getResource("/META-INF/resources/webjars").toExternalForm))

    context setContextPath "/"
    context.setBaseResource(staticResources)
    context.setWelcomeFiles(Array("index.html"))
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    server.setHandler(context)
    server.start
    server.join
  }
}
