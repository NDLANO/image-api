package no.ndla.imageapi

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.imageapi.integration.{AmazonClientComponent, CMDataComponent, DataSourceComponent, ElasticClientComponent}
import no.ndla.imageapi.repository.{ImageRepositoryComponent, SearchIndexerComponent}
import no.ndla.imageapi.service.{ElasticContentIndexComponent, ImageStorageService, ImportServiceComponent, SearchService}
import org.elasticsearch.common.settings.Settings
import org.postgresql.ds.PGPoolingDataSource

import scala.util.Properties.envOrNone

object ComponentRegistry
  extends ElasticClientComponent
  with ElasticContentIndexComponent
  with SearchService
  with DataSourceComponent
  with ImageRepositoryComponent
  with AmazonClientComponent
  with ImageStorageService
  with SearchIndexerComponent
  with CMDataComponent
  with ImportServiceComponent
{
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

  lazy val CMPassword = envOrNone("CM_PASSWORD")
  lazy val CMUser = envOrNone("CM_USER")
  lazy val CMHost = envOrNone("CM_HOST")
  lazy val CMPort = envOrNone("CM_PORT")
  lazy val CMDatabase = envOrNone("CM_DATABASE")
  lazy val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)

  val amazonClient = new AmazonS3Client(new BasicAWSCredentials(ImageApiProperties.StorageAccessKey, ImageApiProperties.StorageSecretKey))
  amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
  lazy val storageName = ImageApiProperties.StorageName

  lazy val elasticContentIndex = new ElasticContentIndex
  lazy val searchService = new ElasticContentSearch
  lazy val imageRepository = new ImageRepository
  lazy val imageStorage = new AmazonImageStorageService
  lazy val searchIndexer = new SearchIndexer
  lazy val importService = new ImportService
}
