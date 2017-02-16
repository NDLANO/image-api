/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi


import com.amazonaws.services.s3.AmazonS3
import no.ndla.imageapi.controller.{HealthController, ImageController, InternController}
import no.ndla.imageapi.integration._
import no.ndla.imageapi.repository._
import no.ndla.imageapi.service._
import no.ndla.imageapi.service.search.{IndexBuilderService, IndexService, SearchConverterService, SearchService}
import no.ndla.network.NdlaClient
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
  extends ElasticClient
    with IndexService
    with SearchService
    with SearchConverterService
    with DataSource
    with ConverterService
    with ValidationService
    with ImageRepository
    with WriteService
    with AmazonClient
    with ImageStorageService
    with IndexBuilderService
    with ImportService
    with MigrationApiClient
    with NdlaClient
    with InternController
    with ImageController
    with TagsService
    with HealthController
    with MockitoSugar
{
  val amazonClient = mock[AmazonS3]

  val dataSource = mock[javax.sql.DataSource]
  val indexService = mock[IndexService]
  val searchService = mock[SearchService]
  val indexBuilderService = mock[IndexBuilderService]
  val imageRepository = mock[ImageRepository]
  val writeService = mock[WriteService]
  val imageStorage = mock[AmazonImageStorageService]

  val importService = mock[ImportService]
  val ndlaClient = mock[NdlaClient]
  val migrationApiClient = mock[MigrationApiClient]
  val imageController = mock[ImageController]
  val internController = mock[InternController]
  val healthController = mock[HealthController]
  val converterService = mock[ConverterService]
  val validationService = mock[ValidationService]
  val tagsService = mock[TagsService]
  val jestClient = mock[NdlaJestClient]
  val searchConverterService = mock[SearchConverterService]
}
