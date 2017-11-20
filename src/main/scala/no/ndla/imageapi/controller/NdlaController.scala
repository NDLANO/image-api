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
import no.ndla.imageapi.model._
import no.ndla.network.{ApplicationUrl, AuthUser, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.servlet.SizeConstraintExceededException
import org.scalatra.{BadRequest, InternalServerError, RequestEntityTooLarge, ScalatraServlet, _}
import java.lang.Math.{max, min}
import scala.util.{Failure, Success, Try}

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
    case v: ValidationException => BadRequest(body = ValidationError(messages = v.errors))
    case a: AccessDeniedException => Forbidden(body = Error(Error.ACCESS_DENIED, a.getMessage))
    case e: IndexNotFoundException => InternalServerError(body = Error.IndexMissingError)
    case i: ImageNotFoundException => NotFound(body = Error(Error.NOT_FOUND, i.getMessage))
    case s: S3UploadException => {
      contentType = formats("json")
      GatewayTimeout(body = Error(Error.GATEWAY_TIMEOUT, s.getMessage))
    }
    case rw: ResultWindowTooLargeException => UnprocessableEntity(body = Error(Error.WINDOW_TOO_LARGE, rw.getMessage))
    case _: SizeConstraintExceededException =>
      contentType = formats("json")
      RequestEntityTooLarge(body = Error.FileTooBigError)
    case t: Throwable => {
      logger.error(Error.GenericError.toString, t)
      InternalServerError(body = Error.GenericError)
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

  def isBoolean(value: String): Boolean = Try(value.toBoolean).isSuccess

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

  def booleanOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): Boolean = {
    val paramValue = paramOrDefault(paramName, default)
    if (!isBoolean(paramValue))
      throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only true or false is allowed.")))

    paramValue.toBoolean
  }

  def paramOrNone(paramName: String)(implicit request: HttpServletRequest): Option[String] = {
    params.get(paramName).map(_.trim).filterNot(_.isEmpty())
  }

  def doubleOrNone(name: String)(implicit request: HttpServletRequest): Option[Double] = {
    paramOrNone(name).flatMap(i => Try(i.toDouble).toOption)
  }

  def intOrNone(name: String)(implicit request: HttpServletRequest): Option[Int] = {
    paramOrNone(name).flatMap(i => Try(i.toInt).toOption)
  }

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String = {
    paramOrNone(paramName).getOrElse(default)
  }

  def doubleInRange(paramName: String, from: Int, to: Int)(implicit request: HttpServletRequest): Option[Double] = {
    doubleOrNone(paramName) match {
      case Some(d) if d >= min(from, to) && d <= max(from, to) => Some(d)
      case Some(d) => throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Must be in range $from-$to but was $d")))
      case None => None
    }
  }

  def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    Try(read[T](json)) match {
      case Success(data) => data
      case Failure(e) =>
        logger.error(e.getMessage, e)
        throw new ValidationException(errors=Seq(ValidationMessage("body", e.getMessage)))
    }
  }
}
