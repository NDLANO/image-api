/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import com.sksamuel.elastic4s.ElasticClient

trait ElasticClientComponent {
  val elasticClient: ElasticClient
}
