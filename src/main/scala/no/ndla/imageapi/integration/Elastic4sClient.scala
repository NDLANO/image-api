/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import javax.naming.directory.InitialDirContext

import com.amazonaws.regions.{Region, Regions}
import com.netaporter.uri.dsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.aws._
import com.sksamuel.elastic4s.http.{HttpClient, HttpExecutable, HttpRequestClient, RequestSuccess}
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.NdlaSearchException

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait Elastic4sClient {
  val e4sClient: NdlaE4sClient
}

case class NdlaE4sClient(httpClient: HttpClient) {
  def execute[T, U](request: T)(implicit exec: HttpExecutable[T, U]): Try[RequestSuccess[U]] = {
    val response = Await.ready(httpClient.execute {
      request
    }, Duration.Inf).value.get

    response match {
      case Success(either) => either match {
        case Right(result) => Success(result)
        case Left(requestFailure) => Failure(NdlaSearchException(requestFailure))
      }
      case Failure(ex) => Failure(ex)
    }
  }
}

object Elastic4sClientFactory {
  def getClient(searchServer: String = ImageApiProperties.SearchServer): NdlaE4sClient = {
    ImageApiProperties.RunWithSignedSearchRequests match {
      case true => NdlaE4sClient(getSigningClient(searchServer))
      case false => NdlaE4sClient(getNonSigningClient(searchServer))
    }
  }

  private def getNonSigningClient(searchServer: String): HttpClient = {
    val uri = ElasticsearchClientUri(searchServer.host.getOrElse("localhost"), searchServer.port.getOrElse(9200))
    HttpClient(uri)
  }

  private def getSigningClient(searchServer: String): HttpClient = {
    // Since elastic4s does not resolve internal CNAME by itself, we do it here
    val in = java.net.InetAddress.getByName(searchServer.host.getOrElse("localhost"))
    val attr = new InitialDirContext().getAttributes("dns:/" + in.getHostName)
    val esEndpoint = attr.get("CNAME").get(0).toString.dropRight(1)

    val elasticSearchUri = s"elasticsearch://$esEndpoint:${searchServer.port.getOrElse(443)}?ssl=true"
    val awsRegion = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_CENTRAL_1)).toString
    setEnv("AWS_DEFAULT_REGION", awsRegion)

    Aws4ElasticClient(elasticSearchUri)
  }

  private def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }

}
