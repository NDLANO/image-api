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
import no.ndla.imageapi.model.{api}
import no.ndla.imageapi.ImageApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.model.{Error, ImageNotFoundException, ValidationException}
import no.ndla.network.{ApplicationUrl, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra._


abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
    ApplicationUrl.set(request)
    logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
    ApplicationUrl.clear
  }

  error {
    case v: ValidationException => BadRequest(api.Error(Error.VALIDATION, v.getMessage))
    case e: IndexNotFoundException => InternalServerError(Error.IndexMissingError)
    case i: ImageNotFoundException => NotFound(api.Error(Error.NOT_FOUND, i.getMessage))
    case t: Throwable => {
      logger.error(Error.GenericError.toString, t)
      halt(status = 500, body = Error.GenericError)
    }
  }

  private val streamRenderer: RenderPipeline = {
    case f: ImageStream =>
      contentType = f.contentType
      org.scalatra.util.io.copy(f.stream, response.getOutputStream)
  }

  override def renderPipeline = streamRenderer orElse super.renderPipeline

  def isNumber(value: String): Boolean = value.forall(_.isDigit)

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    if (!isNumber(paramValue))
      throw new ValidationException(s"Invalid value for $paramName. Only digits are allowed.")

    paramValue.toLong
  }

  def extractIntOpts(paramNames: String*)(implicit request: HttpServletRequest): Seq[Option[Int]] = {
    paramNames.map(paramName => {
      params.get(paramName) match {
        case Some(value) =>
          if (!isNumber(value))
            throw new ValidationException(s"Invalid value for $paramName. Only digits are allowed.")

          Some(value.toInt)
        case _ => None
      }
    })
  }

  def paramAsListOfInt(paramName: String)(implicit request: HttpServletRequest): List[Int] = {
    params.get(paramName).map(param => {
      val paramAsListOfStrings = param.split(",").toList.map(_.trim)
      if (!paramAsListOfStrings.forall(isNumber))
        throw new ValidationException(s"Invalid value for $paramName. Only (list of) digits are allowed.")

      paramAsListOfStrings.map(_.toInt)
    }).getOrElse(List.empty)
  }

}
