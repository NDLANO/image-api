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
      if (indexExists(indexName).getOrElse(false)) {
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

    def findAllIndexes(): Try[Seq[String]] = {
      jestClient.execute(new Stats.Builder().build())
        .map(r => r.getJsonObject.get("indices").getAsJsonObject.entrySet().asScala.map(_.getKey).toSeq)
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
        case None => Success(optIndexName)
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
      val ga = e4sClient.execute{
        getAliases(Nil, List(ImageApiProperties.SearchIndex))
      }.await

      ga match {
        case Right(results) =>
          Success(results.result.mappings.headOption.map((t) => t._1.name))
        case Left(e) =>
          Failure(Ndla4sSearchException(e))
      }
    }

    def indexExists(indexName: String): Try[Boolean] = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()) match {
        case Success(_) => Success(true)
        case Failure(_: ElasticsearchException) => Success(false)
        case Failure(t: Throwable) => Failure(t)
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
