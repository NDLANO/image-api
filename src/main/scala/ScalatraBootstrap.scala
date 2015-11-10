/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
import javax.servlet.ServletContext

import no.ndla.imageapi.{AdminController, ImageController, ImageSwagger, ResourcesApp}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ImageSwagger

  override def init(context: ServletContext) {
    // Mount servlets.
    context.mount(new ImageController, "/images", "images")
    context.mount(new ResourcesApp, "/api-docs")
    context.mount(new AdminController, "/admin")
  }

}
