/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s
import com.sksamuel.elastic4s.mappings.{MappingBuilderFn, MappingDefinition, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists, Stats}
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.{Elastic4sClient, ElasticClient}
import no.ndla.imageapi.model.Language._
import no.ndla.imageapi.model.search.SearchableLanguageFormats
import no.ndla.imageapi.model.{Ndla4sSearchException, domain}
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with SearchConverterService with Elastic4sClient =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    implicit val formats = SearchableLanguageFormats.JSonFormats

    def indexDocument(imageMetaInformation: domain.ImageMetaInformation): Try[domain.ImageMetaInformation] = {
      val source = write(searchConverterService.asSearchableImage(imageMetaInformation))

      val response = e4sClient.execute{
        indexInto(ImageApiProperties.SearchIndex / ImageApiProperties.SearchDocument).doc(source).id(imageMetaInformation.id.get.toString)
      }.await

      response match {
        case Right(_) => Success(imageMetaInformation)
        case Left(requestFailure) => Failure(Ndla4sSearchException(requestFailure))
      }
    }

    def indexDocuments(imageMetaList: List[domain.ImageMetaInformation], indexName: String): Try[Int] = {
      val response = e4sClient.execute{
        bulk(imageMetaList.map(imageMeta => {
          val source = write(searchConverterService.asSearchableImage(imageMeta))
          indexInto(indexName / ImageApiProperties.SearchDocument).doc(source).id(imageMeta.id.get.toString)
        }))
      }.await

      response match {
        case Right(_) => Success(imageMetaList.size)
        case Left(requestFailure) => Failure(Ndla4sSearchException(requestFailure))
      }

    }

    def createIndexWithGeneratedName(): Try[String] = {
      createIndexWithName(ImageApiProperties.SearchIndex + "_" + getTimestamp + "_" + UUID.randomUUID().toString)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute{
          createIndex(indexName)
            .mappings(buildMapping)
            .indexSetting("max_result_window", ImageApiProperties.ElasticSearchIndexMaxResultWindow)
        }.await

        response match {
          case Right(_) => Success(indexName)
          case Left(requestFailure) => Failure(Ndla4sSearchException(requestFailure))
        }

      }
    }

    def findAllIndexes: Try[Seq[String]] = {
      val response = e4sClient.execute{
        catIndices()
      }.await

      response match {
        case Right(results) => Success(results.result.map(index => index.index))
        case Left(requestFailure) => Failure(Ndla4sSearchException(requestFailure))
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val response = (oldIndexName match {
          case None =>
            e4sClient.execute(addAlias(ImageApiProperties.SearchIndex).on(newIndexName))
          case Some(oldIndex) =>
            e4sClient.execute{
              removeAlias(ImageApiProperties.SearchIndex).on(oldIndex)
              addAlias(ImageApiProperties.SearchIndex).on(newIndexName)
            }
        }).await

        response match {
          case Right(resp) => Success(resp)
          case Left(requestFailure) => Failure(Ndla4sSearchException(requestFailure))
        }
      }
    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            val response = e4sClient.execute{
              deleteIndex(indexName)
            }.await

            response match {
              case Right(resp) => Success(resp)
              case Left(requestFailure) => Failure(Ndla4sSearchException(requestFailure))
            }
          }
        }
      }
    }

    def aliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute{
        getAliases(Nil, List(ImageApiProperties.SearchIndex))
      }.await

      response match {
        case Right(results) =>
          Success(results.result.mappings.headOption.map((t) => t._1.name))
        case Left(e) =>
          Failure(Ndla4sSearchException(e))
      }
    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute{
        indexExists(indexName)
      }.await

      response match {
        case Right(resp) if resp.status != 404 => Success(true)
        case Right(_) => Success(false)
        case Left(requestFailure) => Failure(Ndla4sSearchException(requestFailure))
      }
    }

    def buildMapping: MappingDefinition = {
      mapping(ImageApiProperties.SearchDocument).fields(
        intField("id"),
        keywordField("license"),
        intField("imageSize"),
        textField("previewUrl"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        languageSupportedField("titles", keepRaw = true),
        languageSupportedField("alttexts", keepRaw = false),
        languageSupportedField("captions", keepRaw = false),
        languageSupportedField("tags", keepRaw = false)
      )
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = NestedFieldDefinition(fieldName).fields(
        keepRaw match {
          case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw")))
          case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer)
        }
      )

      languageSupportedField
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
