/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext

import no.ndla.imageapi._
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.imageController, "/image-api/v1/images", "images")
    context.mount(ComponentRegistry.internController, "/intern")
    context.mount(ComponentRegistry.healthController, ImageApiProperties.HealthControllerPath)
  }
}
