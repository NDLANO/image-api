/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.swagger

import java.lang.annotation.Annotation
import java.lang.reflect.{Method, Type}

import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations.{Api, ApiOperation}
import io.swagger.models.Operation
import io.swagger.servlet.ReaderContext
import io.swagger.servlet.extensions.{ReaderExtension, ServletReaderExtension}
import io.swagger.util.{PathUtils, ReflectionUtils}


class ScalatraServletReaderExtension extends ReaderExtension with LazyLogging {
  val servletReaderExtension = new ServletReaderExtension

  override def getPriority: Int = -1

  override def getPath(context: ReaderContext, method: Method): String = {
    val apiAnnotation: Api = context.getCls.getAnnotation(classOf[Api])
    val apiOperation: ApiOperation = ReflectionUtils.getAnnotation(method, classOf[ApiOperation])
    val operationPath: String = if (apiOperation == null) method.getName else apiOperation.nickname
    PathUtils.collectPath(context.getParentPath, if (apiAnnotation == null) null else apiAnnotation.value, operationPath)
  }

  override def isReadable(context: ReaderContext): Boolean = servletReaderExtension.isReadable(context)
  override def getHttpMethod(context: ReaderContext, method: Method): String = servletReaderExtension.getHttpMethod(context, method)

  override def setDeprecated(operation: Operation, method: Method): Unit = {}
  override def applyTags(context: ReaderContext, operation: Operation, method: Method): Unit = {}
  override def applyParameters(context: ReaderContext, operation: Operation, `type`: Type, annotations: Array[Annotation]): Unit = {}
  override def applySecurityRequirements(context: ReaderContext, operation: Operation, method: Method): Unit = {}
  override def applySummary(operation: Operation, method: Method): Unit = {}
  override def applyProduces(context: ReaderContext, operation: Operation, method: Method): Unit = {}
  override def applyDescription(operation: Operation, method: Method): Unit = {}
  override def applyOperationId(operation: Operation, method: Method): Unit = {}
  override def applyImplicitParameters(context: ReaderContext, operation: Operation, method: Method): Unit = {}
  override def applySchemes(context: ReaderContext, operation: Operation, method: Method): Unit= {}
  override def applyResponses(context: ReaderContext, operation: Operation, method: Method): Unit = {}
  override def applyConsumes(context: ReaderContext, operation: Operation, method: Method): Unit = {}
  override def applyExtensions(context: ReaderContext, operation: Operation, method: Method): Unit = {}
}
