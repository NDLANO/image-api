/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.imageapi.integration._
import no.ndla.imageapi.repository.{ImageRepositoryComponent, SearchIndexerComponent}
import no.ndla.imageapi.service._
import no.ndla.network.NdlaClient
import org.elasticsearch.common.settings.Settings
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends ElasticClientComponent
  with ElasticContentIndexComponent
  with SearchService
  with DataSourceComponent
  with ImageRepositoryComponent
  with AmazonClientComponent
  with ImageStorageService
  with SearchIndexerComponent
  with NdlaClient
  with MigrationApiClient
  with ImportServiceComponent
  with InternController
  with ImageController
  with ConverterService
  with MappingApiClient
  with TagsService
{
  implicit val swagger = new ImageSwagger

  lazy val elasticClient = ElasticClient.transport(
    Settings.settingsBuilder().put("cluster.name", ImageApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://${ImageApiProperties.SearchHost}:${ImageApiProperties.SearchPort}"))


  val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ImageApiProperties.MetaUserName)
  dataSource.setPassword(ImageApiProperties.MetaPassword)
  dataSource.setDatabaseName(ImageApiProperties.MetaResource)
  dataSource.setServerName(ImageApiProperties.MetaServer)
  dataSource.setPortNumber(ImageApiProperties.MetaPort)
  dataSource.setInitialConnections(ImageApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(ImageApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(ImageApiProperties.MetaSchema)

  val amazonClient = new AmazonS3Client(new BasicAWSCredentials(ImageApiProperties.StorageAccessKey, ImageApiProperties.StorageSecretKey))
  amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
  lazy val storageName = ImageApiProperties.StorageName

  lazy val elasticContentIndex = new ElasticContentIndex
  lazy val searchService = new ElasticContentSearch
  lazy val imageRepository = new ImageRepository
  lazy val imageStorage = new AmazonImageStorageService
  lazy val searchIndexer = new SearchIndexer
  lazy val importService = new ImportService
  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient
  lazy val internController = new InternController
  lazy val imageController = new ImageController
  lazy val resourcesApp = new ResourcesApp
  lazy val converterService = new ConverterService
  lazy val mappingApiClient = new MappingApiClient
  lazy val tagsService = new TagsService
}
