/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats, SearchableTag}
import no.ndla.imageapi.repository.{ImageRepository, Repository}
import org.json4s.native.Serialization.write

trait TagIndexService {
  this: SearchConverterService with IndexService with ImageRepository =>
  val tagIndexService: TagIndexService

  class TagIndexService extends LazyLogging with IndexService[ImageMetaInformation, SearchableTag] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = ImageApiProperties.TagSearchDocument
    override val searchIndex: String = ImageApiProperties.TagSearchIndex
    override val repository: Repository[ImageMetaInformation] = imageRepository

    override def createIndexRequests(domainModel: ImageMetaInformation, indexName: String): Seq[IndexRequest] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      tags.map(t => {
        val source = write(t)
        indexInto(indexName / documentType).doc(source).id(s"${t.language}.${t.tag}")
      })
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        List(
          textField("tag").fields(keywordField("raw")),
          keywordField("language")
        )
      )
    }
  }

}
