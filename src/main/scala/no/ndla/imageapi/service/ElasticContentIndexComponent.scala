package no.ndla.imageapi.service

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, NestedType, StringType}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.ElasticClientComponent
import no.ndla.imageapi.model.{domain}
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait ElasticContentIndexComponent {
  this: ElasticClientComponent with ConverterService =>
  val elasticContentIndex: ElasticContentIndex

  class ElasticContentIndex extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats

    def indexDocument(imageMetaInformation: domain.ImageMetaInformation) = {
      Try {
        aliasTarget.foreach(indexName => {
          elasticClient.execute {
            index into indexName -> ImageApiProperties.SearchDocument source write(converterService.asApiImageMetaInformationWithRelUrl(imageMetaInformation)) id imageMetaInformation.id.get
          }.await
        })
      } match {
        case Success(_) =>
        case Failure(f) => logger.warn(s"Could not add image with id ${imageMetaInformation.id} to search index. Try recreating the index. The error was ${f.getMessage}")
      }
    }

    def indexDocuments(imageMetaList: List[domain.ImageMetaInformation], indexName: String): Unit = {
      elasticClient.execute {
        bulk(imageMetaList.map(imageMeta => {
          index into indexName -> ImageApiProperties.SearchDocument source write(converterService.asApiImageMetaInformationWithRelUrl(imageMeta)) id imageMeta.id.get
        }))
      }.await
    }

    def createIndex(indexName: String) = {
      val existsDefinition = elasticClient.execute {
        index exists indexName.toString
      }.await

      if (!existsDefinition.isExists) {
        elasticClient.execute {
          create index indexName mappings (
            ImageApiProperties.SearchDocument as(
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
              )
            )
        }.await
      }
    }

    def updateAliasTarget(newIndexName: String, oldIndexName: Option[String]) = {
      val existsDefinition = elasticClient.execute {
        index exists newIndexName
      }.await


      if (existsDefinition.isExists) {
        elasticClient.execute {
          oldIndexName.foreach(oldIndexName => {
            remove alias ImageApiProperties.SearchIndex on oldIndexName
          })
          add alias ImageApiProperties.SearchIndex on newIndexName
        }.await
      } else {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
      }
    }

    def deleteIndex(indexName: String) = {
      val existsDefinition = elasticClient.execute {
        index exists indexName
      }.await
      if (existsDefinition.isExists) {
        elasticClient.execute {
          delete index indexName
        }.await
      } else {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }
    }

    def aliasTarget: Option[String] = {
      val res = elasticClient.execute {
        get alias ImageApiProperties.SearchIndex
      }.await
      val aliases = res.getAliases.keysIt()
      aliases.hasNext match {
        case true => Some(aliases.next())
        case false => None
      }
    }
  }

}
