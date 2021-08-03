/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.amazonaws.services.s3.AmazonS3
import com.zaxxer.hikari.HikariDataSource
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.controller.{HealthController, ImageControllerV2, InternController, RawController}
import no.ndla.imageapi.integration._
import no.ndla.imageapi.repository._
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
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with IndexService
    with TagIndexService
    with SearchService
    with ImageSearchService
    with TagSearchService
    with SearchConverterService
    with DataSource
    with ConverterService
    with ValidationService
    with ImageRepository
    with ReadService
    with WriteService
    with AmazonClient
    with ImageStorageService
    with ImageIndexService
    with DraftApiClient
    with NdlaClient
    with InternController
    with ImageControllerV2
    with HealthController
    with RawController
    with TagsService
    with ImageConverter
    with MockitoSugar
    with User
    with Role
    with Clock {
  val amazonClient = mock[AmazonS3]

  val dataSource = mock[HikariDataSource]
  val imageIndexService = mock[ImageIndexService]
  val imageSearchService = mock[ImageSearchService]

  val tagIndexService = mock[TagIndexService]
  val tagSearchService = mock[TagSearchService]

  val imageRepository = mock[ImageRepository]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val imageStorage = mock[AmazonImageStorageService]

  val ndlaClient = mock[NdlaClient]
  val draftApiClient = mock[DraftApiClient]
  val rawController = mock[RawController]
  val internController = mock[InternController]
  val imageControllerV2 = mock[ImageControllerV2]
  val converterService = mock[ConverterService]
  val validationService = mock[ValidationService]
  val tagsService = mock[TagsService]
  val e4sClient = mock[NdlaE4sClient]
  val searchConverterService = mock[SearchConverterService]
  val imageConverter = mock[ImageConverter]
  val healthController = mock[HealthController]

  val clock = mock[SystemClock]
  val authUser = mock[AuthUser]
  val authRole = new AuthRole
}
