/*
 * Part of NDLA image_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.Executors

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.sksamuel.elastic4s.http._
import io.lemonlabs.uri.dsl._
import no.ndla.imageapi.model.NdlaSearchException
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}
import no.ndla.imageapi.ImageApiProperties.{RunWithSignedSearchRequests, SearchServer}
import vc.inreach.aws.request.{AWSSigner, AWSSigningRequestInterceptor}

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
      .ready(
        client.execute(request),
        Duration.Inf
      )
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

  def getClient(searchServer: String = SearchServer): NdlaE4sClient =
    if (RunWithSignedSearchRequests) NdlaE4sClient(getSigningClient(searchServer))
    else NdlaE4sClient(getNonSigningClient(searchServer))

  private def getProperties(searchServer: String, defaultPort: Int) = {
    val scheme = searchServer.schemeOption.getOrElse("http")
    val host = searchServer.hostOption.map(_.toString()).getOrElse("localhost")
    val port = searchServer.port.getOrElse(defaultPort)

    ElasticProperties(s"$scheme://$host:$port?ssl=false")
  }

  /**
    * AWS elasticsearch service requires signed http requests.
    * This method gets a client which will sign the requests using credentials from the server.
    */
  private def getSigningClient(searchServer: String): ElasticClient = ElasticClient(
    getProperties(searchServer, 80),
    httpClientConfigCallback = HttpClientCallbackWithAwsInterceptor,
    requestConfigCallback = RequestConfigCallbackWithTimeout
  )

  /** Get client useful for testing and running in local environments which require no AWS signing. */
  private def getNonSigningClient(searchServer: String): ElasticClient = ElasticClient(
    getProperties(searchServer, 9200),
    requestConfigCallback = RequestConfigCallbackWithTimeout,
    httpClientConfigCallback = NoOpHttpClientConfigCallback
  )

  /** Callback added to all requests that will increase timeout */
  private object RequestConfigCallbackWithTimeout extends RequestConfigCallback {
    override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
      val elasticSearchRequestTimeoutMs = 10000
      requestConfigBuilder.setConnectionRequestTimeout(elasticSearchRequestTimeoutMs)
    }
  }

  /** ConfigCallback to attach to http client that will sign requests going to aws elasticsearch service. */
  private object HttpClientCallbackWithAwsInterceptor extends HttpClientConfigCallback {
    override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
      val defaultChainProvider = new DefaultAWSCredentialsProviderChain
      val region = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_CENTRAL_1)).toString

      val clock = new com.google.common.base.Supplier[LocalDateTime] {
        override def get() = LocalDateTime.now(ZoneOffset.UTC)
      }

      val signer = new AWSSigner(defaultChainProvider, region, "es", clock)
      val interceptor = new AWSSigningRequestInterceptor(signer)
      httpClientBuilder.addInterceptorLast(interceptor)
    }
  }
}
