/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.sksamuel.elastic4s.aws._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import no.ndla.imageapi.ImageApiProperties
import com.netaporter.uri.dsl._

trait Elastic4sClient {
  val e4sClient: HttpClient
}

object Ndla4sFactory {
  def getClient(searchServer: String = ImageApiProperties.SearchServer): HttpClient = {
    ImageApiProperties.RunWithSignedSearchRequests match {
      case true => getSigningClient(searchServer)
      case false => getNonSigningClient(searchServer)
    }
  }

  private def getNonSigningClient(searchServer: String): HttpClient = {
    val uri = ElasticsearchClientUri(searchServer.host.getOrElse("localhost"), searchServer.port.getOrElse(9200))
    HttpClient(uri)
  }

  private def getSigningClient(searchServer: String): HttpClient = {
    val credentials = new DefaultAWSCredentialsProviderChain().getCredentials
    val config = Aws4ElasticConfig(
      s"elasticsearch://${searchServer.host.getOrElse("localhost")}:${searchServer.port.getOrElse(9200)}",
      credentials.getAWSAccessKeyId,
      credentials.getAWSSecretKey,
      ImageApiProperties.SearchRegion)

    Aws4ElasticClient(config)
  }
}
