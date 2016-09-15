/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClientComponent
import no.ndla.imageapi.model.domain
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

    def indexDocuments(imageMetaList: List[domain.ImageMetaInformation], indexName: String): Unit = {
      val bulkBuilder = new Bulk.Builder()
      imageMetaList.foreach(imageMeta => {
        val source = write(converterService.asApiImageMetaInformationWithRelUrl(imageMeta))
        bulkBuilder.addAction(new Index.Builder(source).index(ImageApiProperties.SearchIndex).`type`(ImageApiProperties.SearchDocument).id(imageMeta.id.get.toString).build)
      })
      jestClient.execute(bulkBuilder.build())
    }

    def createIndex(indexName: String) = {
      if (!indexExists(indexName)) {
        jestClient.execute(new CreateIndex.Builder(indexName).build())
        jestClient.execute(new PutMapping.Builder(indexName, ImageApiProperties.SearchDocument, imageMapping).build())
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

        jestClient.execute(modifyAliasRequest)
      } else {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
      }
    }

    def deleteIndex(indexName: String) = {
      if (indexExists(indexName)) {
        jestClient.execute(new DeleteIndex.Builder(indexName).build())
      } else {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }
    }

    def aliasTarget: Option[String] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${ImageApiProperties.SearchIndex}*").build()
      val result = jestClient.execute(getAliasRequest)
      val aliasIterator = result.getJsonObject.entrySet().iterator()
      aliasIterator.hasNext match {
        case true => Some(aliasIterator.next().getKey)
        case false => None
      }
    }

    def indexExists(indexName: String): Boolean = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()).isSucceeded
    }


    val imageMapping =
      s"""
      {
        "${ImageApiProperties.SearchDocument}":{
          "properties":{
            "alttexts":{
              "type":"nested",
              "properties":{
                "alttext":{
                  "type":"string"
                },
                "language":{
                  "type":"string",
                  "index":"not_analyzed"
                }
              }
            },
            "copyright":{
              "type":"nested",
              "properties":{
                "authors":{
                  "type":"nested",
                  "properties":{
                    "name":{
                      "type":"string"
                    },
                    "type":{
                      "type":"string"
                    }
                  }
                },
                "license":{
                  "type":"nested",
                  "properties":{
                    "description":{
                    "type":"string"
                  },
                  "license":{
                    "type":"string",
                    "index":"not_analyzed"
                  },
                  "url":{
                    "type":"string"
                  }
                }
              },
              "origin":{
                "type":"string"
              }
            }
          },
          "id":{
            "type":"integer"
          },
          "images":{
            "type":"nested",
            "properties":{
              "full":{
                "type":"nested",
                "properties":{
                  "contentType":{
                    "type":"string"
                  },"size":{"type":"integer"},"url":{"type":"string"}}},"small":{"type":"nested","properties":{"contentType":{"type":"string"},"size":{"type":"integer"},"url":{"type":"string"}}}}},"metaUrl":{"type":"string","index":"not_analyzed"},"tags":{"type":"nested","properties":{"language":{"type":"string","index":"not_analyzed"},"tags":{"type":"string"}}},"titles":{"type":"nested","properties":{"language":{"type":"string","index":"not_analyzed"},"title":{"type":"string"}}}}}
      }""".stripMargin

  }

}
