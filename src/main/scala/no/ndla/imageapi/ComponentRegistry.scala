/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.zaxxer.hikari.HikariDataSource
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.controller._
import no.ndla.imageapi.integration._
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service._
import no.ndla.imageapi.service.search.{IndexBuilderService, IndexService, SearchConverterService, SearchService}
import no.ndla.network.NdlaClient
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends Elastic4sClient
    with IndexService
    with SearchService
    with SearchConverterService
    with DataSource
    with ImageRepository
    with ReadService
    with WriteService
    with AmazonClient
    with ImageStorageService
    with IndexBuilderService
    with NdlaClient
    with MigrationApiClient
    with DraftApiClient
    with ImportService
    with ConverterService
    with ValidationService
    with TagsService
    with ImageControllerV2
    with RawController
    with InternController
    with HealthController
    with ImageConverter
    with User
    with Role
    with Clock {
  def connectToDatabase(): Unit = ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  implicit val swagger = new ImageSwagger

  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  connectToDatabase()

  val currentRegion: Option[Regions] = Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(currentRegion.getOrElse(Regions.EU_CENTRAL_1))
      .build()

  lazy val indexService = new IndexService
  lazy val searchService = new SearchService
  lazy val indexBuilderService = new IndexBuilderService
  lazy val imageRepository = new ImageRepository
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val validationService = new ValidationService
  lazy val imageStorage = new AmazonImageStorageService
  lazy val importService = new ImportService
  lazy val ndlaClient = new NdlaClient
  lazy val migrationApiClient = new MigrationApiClient
  lazy val draftApiClient = new DraftApiClient
  lazy val imageControllerV2 = new ImageControllerV2
  lazy val rawController = new RawController
  lazy val internController = new InternController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val converterService = new ConverterService
  lazy val tagsService = new TagsService
  lazy val e4sClient = Elastic4sClientFactory.getClient()
  lazy val searchConverterService = new SearchConverterService

  lazy val imageConverter = new ImageConverter
  lazy val authUser = new AuthUser
  lazy val authRole = new AuthRole
  lazy val clock = new SystemClock
}
