package no.ndla.imageapi.integration

import com.sksamuel.elastic4s.ElasticClient

trait ElasticClientComponent {
  val elasticClient: ElasticClient
}
