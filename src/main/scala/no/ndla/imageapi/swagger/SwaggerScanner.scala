/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.swagger

import java.util

import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations.Api
import io.swagger.config.Scanner
import org.reflections.Reflections

class SwaggerScanner(resourcePackage: String, var prettyPrint: Boolean = false) extends Scanner with LazyLogging {
  override def classes(): util.Set[Class[_]] = new Reflections(resourcePackage).getTypesAnnotatedWith(classOf[Api])
  override def setPrettyPrint(shouldPrettyPrint: Boolean): Unit = prettyPrint = shouldPrettyPrint
  override def getPrettyPrint: Boolean = prettyPrint
}
