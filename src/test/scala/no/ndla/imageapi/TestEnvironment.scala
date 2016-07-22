package no.ndla.imageapi

import javax.sql.DataSource

import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.ElasticClient
import no.ndla.imageapi.integration._
import no.ndla.imageapi.repository.{ImageRepositoryComponent, SearchIndexerComponent}
import no.ndla.imageapi.service._
import no.ndla.network.NdlaClient
import org.scalatest.mock.MockitoSugar

trait TestEnvironment
  extends ElasticClientComponent
    with ElasticContentIndexComponent
    with SearchService
    with DataSourceComponent
    with ImageRepositoryComponent
    with AmazonClientComponent
    with ImageStorageService
    with SearchIndexerComponent
    with ImportServiceComponent
    with MigrationApiClient
    with NdlaClient
    with InternController
    with ImageController
    with MockitoSugar
    with ConverterService
    with MappingApiClient
    with TagsService
{
  val storageName = ImageApiProperties.StorageName

  val elasticClient = mock[ElasticClient]
  val amazonClient = mock[AmazonS3Client]

  val dataSource = mock[DataSource]
  val elasticContentIndex = mock[ElasticContentIndex]
  val searchService = mock[ElasticContentSearch]
  val imageRepository = mock[ImageRepository]
  val imageStorage = new AmazonImageStorageService
  val searchIndexer = mock[SearchIndexer]

  val importService = mock[ImportService]
  val ndlaClient = mock[NdlaClient]
  val migrationApiClient = mock[MigrationApiClient]
  val imageController = mock[ImageController]
  val internController = mock[InternController]
  val converterService = mock[ConverterService]
  val mappingApiClient = mock[MappingApiClient]
  val tagsService = mock[TagsService]
}
