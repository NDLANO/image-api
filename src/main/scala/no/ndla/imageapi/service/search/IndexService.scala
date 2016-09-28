/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, StringType}
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClient
import no.ndla.imageapi.model.Language._
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.search.SearchableLanguageFormats
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with SearchConverterService =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    implicit val formats = SearchableLanguageFormats.JSonFormats

    def indexDocument(imageMetaInformation: domain.ImageMetaInformation) = {
      Try {
        val source = write(searchConverterService.asSearchableImage(imageMetaInformation))
        val indexRequest = new Index.Builder(source).index(ImageApiProperties.SearchIndex).`type`(ImageApiProperties.SearchDocument).id(imageMetaInformation.id.get.toString).build
        val result = jestClient.execute(indexRequest)
        if (!result.isSucceeded) {
          logger.warn(s"Received error = ${result.getErrorMessage}")
        }
      } match {
        case Success(_) =>
        case Failure(f) => logger.warn(s"Could not add image with id ${imageMetaInformation.id} to search index. Try recreating the index. The error was ${f.getMessage}")
      }
    }

    def indexDocuments(imageMetaList: List[domain.ImageMetaInformation], indexName: String): Int = {
      val bulkBuilder = new Bulk.Builder()
      imageMetaList.foreach(imageMeta => {
        val source = write(searchConverterService.asSearchableImage(imageMeta))
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(ImageApiProperties.SearchDocument).id(imageMeta.id.get.toString).build)
      })

      val response = jestClient.execute(bulkBuilder.build())
      if (!response.isSucceeded) {
        throw new ElasticsearchException(s"Unable to index documents to ${ImageApiProperties.SearchIndex}", response.getErrorMessage)
      }
      imageMetaList.size
    }

    def createIndex(indexName: String) = {
      if (!indexExists(indexName)) {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.isSucceeded match {
          case false => throw new ElasticsearchException(s"Unable to create index $indexName. ErrorMessage: {}", createIndexResponse.getErrorMessage)
          case true => createMapping(indexName)
        }
      }
    }

    def createMapping(indexName: String) = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, ImageApiProperties.SearchDocument, buildMapping()).build())
      if (!mappingResponse.isSucceeded) {
        throw new ElasticsearchException(s"Unable to create mapping for index $indexName", mappingResponse.getErrorMessage)
      }
    }

    def updateAliasTarget(newIndexName: String, oldIndexName: Option[String]) = {
      if (indexExists(newIndexName)) {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, ImageApiProperties.SearchIndex).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) => {
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, ImageApiProperties.SearchIndex).build()
            ).addAlias(addAliasDefinition).build()
          }
        }

        val response = jestClient.execute(modifyAliasRequest)
        if (!response.isSucceeded) {
          throw new ElasticsearchException(s"Unable to modify alias ${ImageApiProperties.SearchIndex} -> $oldIndexName to ${ImageApiProperties.SearchIndex} -> $newIndexName. ErrorMessage: {}", response.getErrorMessage)
        }
      } else {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
      }
    }

    def deleteIndex(indexName: String) = {
      if (indexExists(indexName)) {
        val response = jestClient.execute(new DeleteIndex.Builder(indexName).build())
        if (!response.isSucceeded) {
          throw new ElasticsearchException(s"Unable to delete index $indexName", response.getErrorMessage)
        }
      } else {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }
    }

    def aliasTarget: Option[String] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${ImageApiProperties.SearchIndex}").build()
      val result = jestClient.execute(getAliasRequest)
      result.isSucceeded match {
        case false => None
        case true => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Some(aliasIterator.next().getKey)
            case false => None
          }
        }
      }
    }

    def indexExists(indexName: String): Boolean = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()).isSucceeded
    }

    def buildMapping(): String = {
      mapping(ImageApiProperties.SearchDocument).fields(
        "id" typed IntegerType,
        "license" typed StringType index "not_analyzed",
        "imageSize" typed IntegerType index "not_analyzed",
        "previewUrl" typed StringType index "not_analyzed",
        languageSupportedField("titles", keepRaw = true),
        languageSupportedField("alttexts", keepRaw = false),
        languageSupportedField("captions", keepRaw = false),
        languageSupportedField("tags", keepRaw = false)
      ).buildWithName.string()
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer fields ("raw" typed StringType index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
    }
  }

}
