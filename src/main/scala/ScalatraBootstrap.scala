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

  override def init(context: ServletContext): Unit = {
    context.mount(ComponentRegistry.imageControllerV2, s"${ImageApiProperties.ImageApiBasePath}/v2/images", "imagesV2")
    context.mount(ComponentRegistry.rawController, ImageApiProperties.RawControllerPath, "raw")
    context.mount(ComponentRegistry.resourcesApp, ImageApiProperties.ApiDocsPath)
    context.mount(ComponentRegistry.internController, "/intern")
    context.mount(ComponentRegistry.healthController, ImageApiProperties.HealthControllerPath)
  }
}
