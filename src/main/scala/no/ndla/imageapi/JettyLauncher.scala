/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

object JettyLauncher extends LazyLogging {

  def main(args: Array[String]) {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    logger.info("Starting DB Migration")
    val DbStartMillis = System.currentTimeMillis()
    DBMigrator.migrate(ComponentRegistry.dataSource)
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - DbStartMillis} ms")

    val startMillis = System.currentTimeMillis()
    val port = ImageApiProperties.ApplicationPort

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")

    ComponentRegistry.searchService.createEmptyIndexIfNoIndexesExist()

    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    val server = new Server(port)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server.join()
  }
}
