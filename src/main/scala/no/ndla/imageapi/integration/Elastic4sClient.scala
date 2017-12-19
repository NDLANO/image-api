/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import javax.naming.directory.InitialDirContext

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.netaporter.uri.dsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.aws._
import com.sksamuel.elastic4s.http.HttpClient
import no.ndla.imageapi.ImageApiProperties

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
    // Since elastic4s does not resolve internal CNAME by itself, we do it here
    val in = java.net.InetAddress.getByName(searchServer.host.getOrElse("localhost"))
    val attr = new InitialDirContext().getAttributes("dns:/"+in.getHostName)
    val esEndpoint = attr.get("CNAME").get(0).toString.dropRight(1)

    val elasticSearchUri = s"elasticsearch://$esEndpoint:${searchServer.port.getOrElse(443)}?ssl=true"
    val awsRegion = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_CENTRAL_1)).toString
    val defaultChainProvider = new DefaultAWSCredentialsProviderChain

    val config = Aws4ElasticConfig(
      endpoint = elasticSearchUri,
      key = defaultChainProvider.getCredentials.getAWSAccessKeyId,
      secret = defaultChainProvider.getCredentials.getAWSSecretKey,
      region = awsRegion
    )

    Aws4ElasticClient(config)
  }
}
