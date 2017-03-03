/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, StringType}
import com.sksamuel.elastic4s.mappings.{MappingContentBuilder, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClient
import no.ndla.imageapi.model.Language._
import no.ndla.imageapi.model.search.SearchableLanguageFormats
import no.ndla.imageapi.model.{NdlaSearchException, domain}
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with SearchConverterService =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    implicit val formats = SearchableLanguageFormats.JSonFormats

    def indexDocument(imageMetaInformation: domain.ImageMetaInformation): Try[domain.ImageMetaInformation] = {
      val source = write(searchConverterService.asSearchableImage(imageMetaInformation))
      val indexRequest = new Index.Builder(source).index(ImageApiProperties.SearchIndex).`type`(ImageApiProperties.SearchDocument).id(imageMetaInformation.id.get.toString).build

      jestClient.execute(indexRequest).map(_ => imageMetaInformation)
    }

    def indexDocuments(imageMetaList: List[domain.ImageMetaInformation], indexName: String): Try[Int] = {
      val bulkBuilder = new Bulk.Builder()
      imageMetaList.foreach(imageMeta => {
        val source = write(searchConverterService.asSearchableImage(imageMeta))
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(ImageApiProperties.SearchDocument).id(imageMeta.id.get.toString).build)
      })

      val response = jestClient.execute(bulkBuilder.build())
      response.map(_ => {
        logger.info(s"Indexed ${imageMetaList.size} documents")
        imageMetaList.size
      })
    }

    def createIndex(): Try[String] = {
      createIndexWithName(ImageApiProperties.SearchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.map(_ => createMapping(indexName)).map(_ => indexName)
      }
    }

    def createMapping(indexName: String): Try[String] = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, ImageApiProperties.SearchDocument, buildMapping()).build())
      mappingResponse.map(_ => indexName)
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
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
      }
    }

    def deleteIndex(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success()
        case Some(indexName) => {
          if (!indexExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            jestClient.execute(new DeleteIndex.Builder(indexName).build())
          }
        }
      }
    }

    def aliasTarget: Try[Option[String]] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${ImageApiProperties.SearchIndex}").build()
      jestClient.execute(getAliasRequest) match {
        case Success(result) => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Success(Some(aliasIterator.next().getKey))
            case false => Success(None)
          }
        }
        case Failure(_: NdlaSearchException) => Success(None)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def indexExists(indexName: String): Try[Boolean] = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()) match {
        case Success(_) => Success(true)
        case Failure(_: ElasticsearchException) => Success(false)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def buildMapping(): String = {
      MappingContentBuilder.buildWithName(mapping(ImageApiProperties.SearchDocument).fields(
        intField("id"),
        keywordField("license") index "not_analyzed",
        intField("imageSize") index "not_analyzed",
        textField("previewUrl") index "not_analyzed",
        languageSupportedField("titles", keepRaw = true),
        languageSupportedField("alttexts", keepRaw = false),
        languageSupportedField("captions", keepRaw = false),
        languageSupportedField("tags", keepRaw = false)
      ), ImageApiProperties.SearchDocument).string()
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer fields (keywordField("raw") index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
