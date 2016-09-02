/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import java.time.{LocalDateTime, ZoneOffset}

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.google.common.base.Supplier
import io.searchbox.client.JestClient
import io.searchbox.client.config.HttpClientConfig
import no.ndla.imageapi.ImageApiProperties
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import vc.inreach.aws.request.{AWSSigner, AWSSigningRequestInterceptor}

trait ElasticClientComponent {
  val jestClient: JestClient
}

object JestClientFactory {


  def getClient: JestClient = {
    val clock: Supplier[LocalDateTime] = new Supplier[LocalDateTime] {
      override def get(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
    }

    val service = "es"
    val region = ImageApiProperties.SearchRegion
    val credentialsProvider = new AWSCredentialsProvider {
      override def refresh(): Unit = {}

      override def getCredentials: AWSCredentials = new BasicAWSCredentials("AKIAJGYNKKHCFWOE7TNA", "dA1wW+tBdZZYZcecfmsRgfQBAvp44lw/Jty4Ur6l")
    }

    val awsSigner = new AWSSigner(credentialsProvider, region, service, clock);
    val requestInterceptor = new AWSSigningRequestInterceptor(awsSigner)

    val factory = new io.searchbox.client.JestClientFactory() {
      override def configureHttpClient(builder: HttpClientBuilder): HttpClientBuilder = {
        builder.addInterceptorLast(requestInterceptor)
        builder
      }

      override def configureHttpClient(builder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
        builder.addInterceptorLast(requestInterceptor)
        builder
      }
    }

    factory.setHttpClientConfig(new HttpClientConfig.Builder(ImageApiProperties.SearchServer).build())
    factory.getObject
  }

  def getLocalClient: JestClient = {
    val factory = new io.searchbox.client.JestClientFactory()
    factory.setHttpClientConfig(new HttpClientConfig.Builder(ImageApiProperties.SearchServer).build())
    factory.getObject
  }
}
