/*
 * Part of NDLA image-api.
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
import no.ndla.imageapi.service.search.{
  ImageIndexService,
  ImageSearchService,
  IndexService,
  SearchConverterService,
  SearchService,
  TagIndexService,
  TagSearchService
}
import no.ndla.network.NdlaClient
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends Elastic4sClient
    with IndexService
    with TagIndexService
    with ImageIndexService
    with SearchService
    with ImageSearchService
    with TagSearchService
    with SearchConverterService
    with DataSource
    with ImageRepository
    with ReadService
    with WriteService
    with AmazonClient
    with ImageStorageService
    with NdlaClient
    with DraftApiClient
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

  lazy val imageIndexService = new ImageIndexService
  lazy val imageSearchService = new ImageSearchService
  lazy val tagIndexService = new TagIndexService
  lazy val tagSearchService = new TagSearchService
  lazy val imageRepository = new ImageRepository
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val validationService = new ValidationService
  lazy val imageStorage = new AmazonImageStorageService
  lazy val ndlaClient = new NdlaClient
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
