/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicTemplateRequest
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats}
import no.ndla.imageapi.repository.{ImageRepository, Repository}
import org.json4s.native.Serialization.write

trait ImageIndexService {
  this: SearchConverterService with IndexService with ImageRepository =>
  val imageIndexService: ImageIndexService

  class ImageIndexService extends LazyLogging with IndexService[ImageMetaInformation, SearchableImage] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = ImageApiProperties.SearchDocument
    override val searchIndex: String = ImageApiProperties.SearchIndex
    override val repository: Repository[ImageMetaInformation] = imageRepository

    override def createIndexRequests(domainModel: ImageMetaInformation, indexName: String): Seq[IndexRequest] = {
      val source = write(searchConverterService.asSearchableImage(domainModel))
      Seq(indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString))
    }

    def getMapping: MappingDefinition = {
      val fields: Seq[FieldDefinition] = List(
        intField("id"),
        keywordField("license"),
        intField("imageSize"),
        textField("previewUrl"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        keywordField("modelReleased"),
        textField("editorNotes"),
      )

      val dynamics: Seq[DynamicTemplateRequest] = generateLanguageSupportedDynamicTemplates("titles", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("alttexts", keepRaw = false) ++
        generateLanguageSupportedDynamicTemplates("captions", keepRaw = false) ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = false)

      mapping(documentType).fields(fields).dynamicTemplates(dynamics)
    }
  }

}
