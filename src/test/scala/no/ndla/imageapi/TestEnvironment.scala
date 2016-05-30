package no.ndla.imageapi

import javax.sql.DataSource

import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.ElasticClient
import no.ndla.imageapi.integration.{AmazonClientComponent, DataSourceComponent, ElasticClientComponent}
import no.ndla.imageapi.repository.{ImageRepositoryComponent, SearchIndexerComponent}
import no.ndla.imageapi.service.{AmazonImageStorageComponent, ElasticContentIndexComponent, ElasticContentSearchComponent}
import org.scalatest.mock.MockitoSugar

trait TestEnvironment
  extends ElasticClientComponent
    with ElasticContentIndexComponent
    with ElasticContentSearchComponent
    with DataSourceComponent
    with ImageRepositoryComponent
    with AmazonClientComponent
    with AmazonImageStorageComponent
    with SearchIndexerComponent
    with MockitoSugar
{
  val storageName = ImageApiProperties.StorageName

  val elasticClient = mock[ElasticClient]
  val amazonClient = mock[AmazonS3Client]

  val dataSource = mock[DataSource]
  val elasticContentIndex = mock[ElasticContentIndex]
  val elasticContentSearch = mock[ElasticContentSearch]
  val imageRepository = mock[ImageRepository]
  val amazonImageStorage = new AmazonImageStorage
  val searchIndexer = mock[SearchIndexer]
}
