package no.ndla.imageapi.integration

import com.sksamuel.elastic4s.mappings.FieldType.{StringType, NestedType, IntegerType}
import com.sksamuel.elastic4s.{ElasticsearchClientUri, ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.business.{IndexMeta}
import no.ndla.imageapi.model.ImageMetaInformation
import org.elasticsearch.common.settings.ImmutableSettings
import org.json4s.native.Serialization._

class ElasticIndexMeta(clusterName:String, clusterHost:String, clusterPort:String) extends IndexMeta with LazyLogging {

  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(s"elasticsearch://$clusterHost:$clusterPort"))

  override def indexDocuments(imageMetaList: List[ImageMetaInformation], indexName: String): Unit = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    client.execute{
      bulk(imageMetaList.map(imageMeta => {
        index into indexName -> ImageApiProperties.SearchDocument source write(imageMeta) id imageMeta.id
      }))
    }.await
  }

  override def createIndex(indexName: String) = {
    val existsDefinition = client.execute{
      index exists indexName.toString
    }.await

    if(!existsDefinition.isExists){
      client.execute {
        create index indexName mappings(
          ImageApiProperties.SearchDocument as (
            "id" typed IntegerType,
            "titles" typed NestedType as (
              "title" typed StringType,
              "language" typed StringType index "not_analyzed"
              ),
            "alttexts" typed NestedType as (
              "alttext" typed StringType,
              "language" typed StringType index "not_analyzed"
              ),
            "images" typed NestedType as (
              "small" typed NestedType as (
                "url" typed StringType,
                "size" typed IntegerType index "not_analyzed",
                "contentType" typed StringType
                ),
              "full" typed NestedType as (
                "url" typed StringType,
                "size" typed IntegerType index "not_analyzed",
                "contentType" typed StringType
                )
              ),
            "copyright" typed NestedType as (
              "license" typed NestedType as (
                "license" typed StringType index "not_analyzed",
                "description" typed StringType,
                "url" typed StringType
                ),
              "origin" typed StringType,
              "authors" typed NestedType as (
                "type" typed StringType,
                "name" typed StringType
                )
              ),
            "tags" typed NestedType as (
              "tag" typed StringType,
              "language" typed StringType index "not_analyzed"
              )
            )
          )
      }.await
    }
  }

  override def updateAliasTarget(newIndexName: String, oldIndexName: Option[String]) = {
    val existsDefinition = client.execute{
      index exists newIndexName
    }.await


    if(existsDefinition.isExists) {
      client.execute{
        oldIndexName.foreach(oldIndexName => {
          remove alias ImageApiProperties.SearchIndex on oldIndexName
        })
        add alias ImageApiProperties.SearchIndex on newIndexName
      }.await
    } else {
      throw new IllegalArgumentException(s"No such index: $newIndexName")
    }
  }

  override def deleteIndex(indexName: String) = {
    val existsDefinition = client.execute{
      index exists indexName
    }.await
    if(existsDefinition.isExists) {
      client.execute {
        delete index indexName
      }.await
    } else {
      throw new IllegalArgumentException(s"No such index: $indexName")
    }
  }

  override def aliasTarget: Option[String] = {
    val res = client.execute {
      get alias ImageApiProperties.SearchIndex
    }.await
    val aliases = res.getAliases.keysIt()
    aliases.hasNext match {
      case true => Some(aliases.next())
      case false => None
    }
  }
}
