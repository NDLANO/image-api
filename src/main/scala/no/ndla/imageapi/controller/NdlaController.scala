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
import no.ndla.imageapi.ImageApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.imageapi.model.api.{Error, ValidationError}
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.model.{AccessDeniedException, ImageNotFoundException, ValidationException, ValidationMessage}
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.servlet.SizeConstraintExceededException
import org.scalatra.{BadRequest, InternalServerError, RequestEntityTooLarge, ScalatraServlet, _}

import scala.util.Try

abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
    ApplicationUrl.set(request)
    AuthUser.set(request)
    logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
    ApplicationUrl.clear
    AuthUser.clear()
  }

  error {
    case v: ValidationException => BadRequest(body=ValidationError(messages=v.errors))
    case a: AccessDeniedException => Forbidden(body = Error(Error.ACCESS_DENIED, a.getMessage))
    case e: IndexNotFoundException => InternalServerError(Error.IndexMissingError)
    case i: ImageNotFoundException => NotFound(Error(Error.NOT_FOUND, i.getMessage))
    case _: SizeConstraintExceededException =>
      contentType = formats("json")
      RequestEntityTooLarge(body=Error.FileTooBigError)
    case t: Throwable => {
      logger.error(Error.GenericError.toString, t)
      InternalServerError(body=Error.GenericError)
    }
  }

  private val streamRenderer: RenderPipeline = {
    case f: ImageStream =>
      contentType = f.contentType
      org.scalatra.util.io.copy(f.stream, response.getOutputStream)
  }

  override def renderPipeline = streamRenderer orElse super.renderPipeline

  def isInteger(value: String): Boolean = value.forall(_.isDigit)

  def isDouble(value: String): Boolean = Try(value.toDouble).isSuccess

  def intOrNone(name: String)(implicit request: HttpServletRequest): Option[Int] =
    params.get(name).flatMap(i => Try(i.toInt).toOption)

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    if (!isInteger(paramValue))
      throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))

    paramValue.toLong
  }

  def extractDoubleOpts(paramNames: String*)(implicit request: HttpServletRequest): Seq[Option[Double]] = {
    paramNames.map(paramName => {
      params.get(paramName) match {
        case Some(value) =>
          if (!isDouble(value))
            throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only numbers are allowed.")))

          Some(value.toDouble)
        case _ => None
      }
    })
  }

  def paramAsListOfInt(paramName: String)(implicit request: HttpServletRequest): List[Int] = {
    params.get(paramName).map(param => {
      val paramAsListOfStrings = param.split(",").toList.map(_.trim)
      if (!paramAsListOfStrings.forall(isInteger))
        throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")))

      paramAsListOfStrings.map(_.toInt)
    }).getOrElse(List.empty)
  }

}
