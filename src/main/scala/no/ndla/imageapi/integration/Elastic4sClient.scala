/*
 * Part of NDLA image_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import java.util.concurrent.Executors
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.sksamuel.elastic4s.aws._
import com.sksamuel.elastic4s.http._
import io.lemonlabs.uri.dsl._
import no.ndla.imageapi.ImageApiProperties.{RunWithSignedSearchRequests, SearchServer}
import no.ndla.imageapi.model.NdlaSearchException
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpRequestInterceptor}
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

trait Elastic4sClient {
  val e4sClient: NdlaE4sClient
}

case class NdlaE4sClient(client: ElasticClient) {

  def execute[T, U](request: T)(implicit handler: Handler[T, U], mf: Manifest[U]): Try[RequestSuccess[U]] = {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
    val response = Await
      .ready(client.execute {
        request
      }, Duration.Inf)
      .value
      .get

    response match {
      case Success(result) =>
        result match {
          case failure: RequestFailure   => Failure(NdlaSearchException(failure))
          case result: RequestSuccess[U] => Success(result)
        }
      case Failure(ex) => Failure(ex)
    }
  }
}

object Elastic4sClientFactory {

  def getClient(searchServer: String = SearchServer): NdlaE4sClient = {
    RunWithSignedSearchRequests match {
      case true  => NdlaE4sClient(getSigningClient(searchServer))
      case false => NdlaE4sClient(getNonSigningClient(searchServer))
    }
  }

  private object RequestConfigCallbackWithTimeout extends RequestConfigCallback {
    override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
      val elasticSearchRequestTimeoutMs = 10000
      requestConfigBuilder.setConnectionRequestTimeout(elasticSearchRequestTimeoutMs)
    }
  }

  private object HttpClientCallbackWithAwsInterceptor extends HttpClientConfigCallback {
    override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
      httpClientBuilder.addInterceptorLast(new AwsHttpInterceptor)
    }
  }

  /** This is the same code as [[Aws4ElasticClient]] uses,
    * however [[Aws4ElasticClient]] does not expose a way to configure [[RequestConfigCallback]]
    * This is subject to change in the near future (See elastic4s PR#1322)
    * When that happens we should go back to using the [[Aws4ElasticClient]] instead of this */
  private class AwsHttpInterceptor extends HttpRequestInterceptor {
    private val defaultChainProvider = new DefaultAWSCredentialsProviderChain
    private val region = sys.env("AWS_DEFAULT_REGION")
    private val signer = new Aws4RequestSigner(defaultChainProvider, region)

    override def process(request: HttpRequest, context: HttpContext): Unit = signer.withAws4Headers(request)
  }

  private def getNonSigningClient(searchServer: String): ElasticClient = {
    val properties = ElasticProperties(
      searchServer
        .withScheme(searchServer.schemeOption.getOrElse("http"))
        .withHost(searchServer.hostOption.map(_.toString).getOrElse("localhost"))
        .withPort(searchServer.port.getOrElse(9200)))

    ElasticClient(properties,
                  requestConfigCallback = RequestConfigCallbackWithTimeout,
                  httpClientConfigCallback = NoOpHttpClientConfigCallback)
  }

  private def getSigningClient(searchServer: String): ElasticClient = {
    val elasticSearchUri =
      s"${searchServer.schemeOption.getOrElse("http")}://${searchServer.hostOption
        .map(_.toString)
        .getOrElse("localhost")}:${searchServer.port.getOrElse(80)}?ssl=false"

    val awsRegion = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_CENTRAL_1)).toString
    setEnv("AWS_DEFAULT_REGION", awsRegion)

    val properties = ElasticProperties(elasticSearchUri)

    ElasticClient(properties,
                  httpClientConfigCallback = HttpClientCallbackWithAwsInterceptor,
                  requestConfigCallback = RequestConfigCallbackWithTimeout)
  }

  private def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }
}
