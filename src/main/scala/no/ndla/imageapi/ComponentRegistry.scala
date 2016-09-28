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
import no.ndla.imageapi.controller.{HealthController, ImageController, InternController}
import no.ndla.imageapi.integration._
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service._
import no.ndla.imageapi.service.search.{IndexBuilderService, IndexService, SearchConverterService, SearchService}
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends ElasticClient
  with IndexService
  with SearchService
  with SearchConverterService
  with DataSource
  with ImageRepository
  with AmazonClient
  with ImageStorageService
  with IndexBuilderService
  with NdlaClient
  with MigrationApiClient
  with ImportService
  with InternController
  with ImageController
  with ConverterService
  with MappingApiClient
  with TagsService
  with HealthController
{
  implicit val swagger = new ImageSwagger

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

  lazy val indexService = new IndexService
  lazy val searchService = new SearchService
  lazy val indexBuilderService = new IndexBuilderService
  lazy val imageRepository = new ImageRepository
  lazy val imageStorage = new AmazonImageStorageService
  lazy val importService = new ImportService
  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient
  lazy val internController = new InternController
  lazy val imageController = new ImageController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val converterService = new ConverterService
  lazy val mappingApiClient = new MappingApiClient
  lazy val tagsService = new TagsService
  lazy val jestClient = JestClientFactory.getClient()
  lazy val searchConverterService = new SearchConverterService
}
