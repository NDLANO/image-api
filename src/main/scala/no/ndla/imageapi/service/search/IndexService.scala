/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}

import com.sksamuel.elastic4s.alias.AliasAction
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.mappings.{MappingDefinition, NestedField}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.Elastic4sClient
import no.ndla.imageapi.model.Language._
import no.ndla.imageapi.model.{ElasticIndexingException, domain}
import no.ndla.imageapi.model.search.SearchableLanguageFormats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: SearchConverterService with Elastic4sClient =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    implicit val formats = SearchableLanguageFormats.JSonFormats

    def indexDocument(imageMetaInformation: domain.ImageMetaInformation): Try[domain.ImageMetaInformation] = {
      val source = write(searchConverterService.asSearchableImage(imageMetaInformation))

      val response = e4sClient.execute {
        indexInto(ImageApiProperties.SearchIndex / ImageApiProperties.SearchDocument)
          .doc(source)
          .id(imageMetaInformation.id.get.toString)
      }

      response match {
        case Success(_)  => Success(imageMetaInformation)
        case Failure(ex) => Failure(ex)
      }
    }

    def indexDocuments(imageMetaList: List[domain.ImageMetaInformation], indexName: String): Try[Int] = {
      if (imageMetaList.isEmpty) {
        Success(0)
      } else {
        val response = e4sClient.execute {
          bulk(imageMetaList.map(imageMeta => {
            val source = write(searchConverterService.asSearchableImage(imageMeta))
            indexInto(indexName / ImageApiProperties.SearchDocument).doc(source).id(imageMeta.id.get.toString)
          }))
        }

        response match {
          case Success(RequestSuccess(_, _, _, result)) if !result.errors =>
            logger.info(s"Indexed ${imageMetaList.size} documents")
            Success(imageMetaList.size)
          case Success(RequestSuccess(_, _, _, result)) =>
            val failed = result.items.collect {
              case item if item.error.isDefined => s"'${item.id}: ${item.error.get.reason}'"
            }

            logger.error(s"Failed to index ${failed.length} items: ${failed.mkString(", ")}")
            Failure(ElasticIndexingException(s"Failed to index ${failed.size}/${imageMetaList.size} images"))
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def createIndexWithGeneratedName(): Try[String] = {
      createIndexWithName(ImageApiProperties.SearchIndex + "_" + getTimestamp + "_" + UUID.randomUUID().toString)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          createIndex(indexName)
            .mappings(buildMapping)
            .indexSetting("max_result_window", ImageApiProperties.ElasticSearchIndexMaxResultWindow)
        }

        response match {
          case Success(_)  => Success(indexName)
          case Failure(ex) => Failure(ex)
        }

      }
    }

    def findAllIndexes(indexName: String): Try[Seq[String]] = {
      val response = e4sClient.execute {
        catIndices()
      }

      response match {
        case Success(results) =>
          Success(results.result.map(index => index.index).filter(_.startsWith(indexName)))
        case Failure(ex) =>
          Failure(ex)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val actions = oldIndexName match {
          case None =>
            List[AliasAction](addAlias(ImageApiProperties.SearchIndex).on(newIndexName))
          case Some(oldIndex) =>
            List[AliasAction](removeAlias(ImageApiProperties.SearchIndex).on(oldIndex),
                              addAlias(ImageApiProperties.SearchIndex).on(newIndexName))
        }

        e4sClient.execute(aliases(actions)) match {
          case Success(_) =>
            logger.info("Alias target updated successfully, deleting other indexes.")
            cleanupIndexes()
          case Failure(ex) =>
            logger.error("Could not update alias target.")
            Failure(ex)
        }
      }
    }

    /**
      * Deletes every index that is not in use by this indexService.
      * Only indexes starting with indexName are deleted.
      *
      * @param indexName Start of index names that is deleted if not aliased.
      * @return Name of aliasTarget.
      */
    def cleanupIndexes(indexName: String = ImageApiProperties.SearchIndex): Try[String] = {
      e4sClient.execute(getAliases()) match {
        case Success(s) =>
          val indexes = s.result.mappings.filter {
            case (index, _) => index.name.startsWith(indexName)
          }

          val unreferencedIndexes = indexes.collect {
            case (index, aliasesForIndex) if aliasesForIndex.isEmpty => index.name
          }

          val indexesWithAlias = indexes.collect {
            case (index, aliasesForIndex) if aliasesForIndex.nonEmpty => index.name
          }

          val (aliasTarget, aliasIndexesToDelete) = indexesWithAlias match {
            case head :: tail =>
              (head, tail)
            case _ =>
              logger.warn("No alias found, when attempting to clean up indexes.")
              ("", List.empty)
          }

          val toDelete = unreferencedIndexes ++ aliasIndexesToDelete

          if (toDelete.isEmpty) {
            logger.info("No indexes to be deleted.")
            Success(aliasTarget)
          } else {
            e4sClient.execute {
              deleteIndex(toDelete)
            } match {
              case Success(_) =>
                logger.info(s"Successfully deleted unreferenced and redundant indexes.")
                Success(aliasTarget)
              case Failure(ex) =>
                logger.error("Could not delete unreferenced and redundant indexes.")
                Failure(ex)
            }
          }
        case Failure(ex) =>
          logger.warn("Could not fetch aliases after updating alias.")
          Failure(ex)
      }

    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.execute {
              deleteIndex(indexName)
            }
          }
        }
      }
    }

    def aliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute {
        getAliases(Nil, List(ImageApiProperties.SearchIndex))
      }

      response match {
        case Success(results) => Success(results.result.mappings.headOption.map(t => t._1.name))
        case Failure(ex)      => Failure(ex)
      }
    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_)                          => Success(false)
        case Failure(ex)                         => Failure(ex)
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
      val languageSupportedField = NestedField(fieldName).fields(
        keepRaw match {
          case true =>
            languageAnalyzers.map(langAnalyzer =>
              textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw")))
          case false =>
            languageAnalyzers.map(langAnalyzer =>
              textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer))
        }
      )

      languageSupportedField
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
