/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, NestedType, StringType}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClientComponent
import no.ndla.imageapi.model.domain
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait ElasticContentIndexComponent {
  this: ElasticClientComponent with ConverterService =>
  val elasticContentIndex: ElasticContentIndex

  class ElasticContentIndex extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats

    def indexDocument(imageMetaInformation: domain.ImageMetaInformation) = {
      Try {
        val source = write(converterService.asApiImageMetaInformationWithRelUrl(imageMetaInformation))
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
        val source = write(converterService.asApiImageMetaInformationWithRelUrl(imageMeta))
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
          case false => throw new ElasticsearchException(s"Unable to create index $indexName", createIndexResponse.getErrorMessage)
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
          throw new ElasticsearchException(s"Unable to modify alias ${ImageApiProperties.SearchIndex} -> $oldIndexName to ${ImageApiProperties.SearchIndex} -> $newIndexName", response.getErrorMessage)
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
        "metaUrl" typed StringType index "not_analyzed",
        "titles" typed NestedType as(
          "title" typed StringType,
          "language" typed StringType index "not_analyzed"
          ),
        "alttexts" typed NestedType as(
          "alttext" typed StringType,
          "language" typed StringType index "not_analyzed"
          ),
        "captions" typed NestedType as(
          "caption" typed StringType,
          "language" typed StringType index "not_analyzed"
          ),
        "images" typed NestedType as(
          "small" typed NestedType as(
            "url" typed StringType,
            "size" typed IntegerType index "not_analyzed",
            "contentType" typed StringType
            ),
          "full" typed NestedType as(
            "url" typed StringType,
            "size" typed IntegerType index "not_analyzed",
            "contentType" typed StringType
            )
          ),
        "copyright" typed NestedType as(
          "license" typed NestedType as(
            "license" typed StringType index "not_analyzed",
            "description" typed StringType,
            "url" typed StringType
            ),
          "origin" typed StringType,
          "authors" typed NestedType as(
            "type" typed StringType,
            "name" typed StringType
            )
          ),
        "tags" typed NestedType as(
          "tags" typed StringType,
          "language" typed StringType index "not_analyzed"
          )
      ).buildWithName.string()
    }
  }

}
