package no.ndla.imageapi

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.imageapi.integration.{AmazonClientComponent, CMDataComponent, DataSourceComponent, ElasticClientComponent}
import no.ndla.imageapi.repository.{ImageRepositoryComponent, SearchIndexerComponent}
import no.ndla.imageapi.service.{AmazonImageStorageComponent, ElasticContentIndexComponent, ElasticContentSearchComponent, ImportServiceComponent}
import org.elasticsearch.common.settings.ImmutableSettings
import org.postgresql.ds.PGPoolingDataSource


object ComponentRegistry
  extends ElasticClientComponent
  with ElasticContentIndexComponent
  with ElasticContentSearchComponent
  with DataSourceComponent
  with ImageRepositoryComponent
  with AmazonClientComponent
  with AmazonImageStorageComponent
  with SearchIndexerComponent
  with CMDataComponent
  with ImportServiceComponent
{
  lazy val elasticClient = ElasticClient.remote(ImmutableSettings.settingsBuilder().put("cluster.name", ImageApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://$ImageApiProperties.SearchHost:$ImageApiProperties.SearchPort"))

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ImageApiProperties.MetaUserName)
  dataSource.setPassword(ImageApiProperties.MetaPassword)
  dataSource.setDatabaseName(ImageApiProperties.MetaResource)
  dataSource.setServerName(ImageApiProperties.MetaServer)
  dataSource.setPortNumber(ImageApiProperties.MetaPort)
  dataSource.setInitialConnections(ImageApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(ImageApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(ImageApiProperties.MetaSchema)

  lazy val CMPassword = scala.util.Properties.envOrNone("CM_PASSWORD")
  lazy val CMUser = scala.util.Properties.envOrNone("CM_USER")
  lazy val CMHost = scala.util.Properties.envOrNone("CM_HOST")
  lazy val CMPort = scala.util.Properties.envOrNone("CM_PORT")
  lazy val CMDatabase = scala.util.Properties.envOrNone("CM_DATABASE")
  lazy val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)

  lazy val amazonClient = new AmazonS3Client(new BasicAWSCredentials(ImageApiProperties.StorageAccessKey, ImageApiProperties.StorageSecretKey))
  amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
  lazy val storageName = ImageApiProperties.StorageName

  lazy val elasticContentIndex = new ElasticContentIndex
  lazy val elasticContentSearch = new ElasticContentSearch
  lazy val imageRepository = new ImageRepository
  lazy val amazonImageStorage = new AmazonImageStorage
  lazy val searchIndexer = new SearchIndexer
  lazy val importService = new ImportService
}
