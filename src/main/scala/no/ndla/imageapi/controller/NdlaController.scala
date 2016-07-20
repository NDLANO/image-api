/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.{Error, ValidationException}
import no.ndla.logging.LoggerContext
import no.ndla.network.ApplicationUrl
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.NativeJsonSupport

abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
    logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
  }

  after() {
    LoggerContext.clearCorrelationID
    ApplicationUrl.clear
  }

  error {
    case v: ValidationException => halt(status = 400, body = Error(Error.VALIDATION, v.getMessage))
    case e: IndexNotFoundException => halt(status = 500, body = Error.IndexMissingError)
    case t: Throwable => {
      logger.error(Error.GenericError.toString, t)
      halt(status = 500, body = Error.GenericError)
    }
  }

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    paramValue.forall(_.isDigit) match {
      case true => paramValue.toLong
      case false => throw new ValidationException(s"Invalid value for $paramName. Only digits are allowed.")
    }
  }
}
