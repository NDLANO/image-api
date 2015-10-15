/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
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
    logger.info(io.Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    ImageApiProperties.verify()

    val startMillis = System.currentTimeMillis();
    val port = ImageApiProperties.getInt("APPLICATION_PORT")

    val server = new Server(port)
    val context = new WebAppContext()

    val staticResources = new ResourceCollection(Array(
      getClass.getResource("/image-api").toExternalForm,
      getClass.getResource("/META-INF/resources/webjars").toExternalForm))

    context setContextPath "/"
    context.setVirtualHosts(ImageApiProperties.Domains)
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
