/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import no.ndla.imageapi.controller.{HealthController, ImageController, InternController, RawController}
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
  with ConverterService
  with TagsService
  with ImageController
  with RawController
  with InternController
  with HealthController
  with ImageConverter
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

  val amazonClient = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()

  lazy val indexService = new IndexService
  lazy val searchService = new SearchService
  lazy val indexBuilderService = new IndexBuilderService
  lazy val imageRepository = new ImageRepository
  lazy val imageStorage = new AmazonImageStorageService
  lazy val importService = new ImportService
  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient
  lazy val imageController = new ImageController
  lazy val rawController = new RawController
  lazy val internController = new InternController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val converterService = new ConverterService
  lazy val tagsService = new TagsService
  lazy val jestClient = JestClientFactory.getClient()
  lazy val searchConverterService = new SearchConverterService

  lazy val imageConverter = new ImageConverter
}
