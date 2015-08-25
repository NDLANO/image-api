package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object JettyLauncher extends LazyLogging {
  // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    val startMillis = System.currentTimeMillis();
    val port = if (System.getenv("PORT") != null) System.getenv("PORT").toInt else 80

    val server = new Server(port)
    val context = new WebAppContext()

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

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server.join
  }
}
