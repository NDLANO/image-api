/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
import javax.servlet.ServletContext

import no.ndla.imageapi._
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.imageController, "/images", "images")
    context.mount(ComponentRegistry.resourcesApp, "/api-docs")
    context.mount(ComponentRegistry.internController, "/intern")
  }
}
