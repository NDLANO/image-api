package no.ndla.imageapi.integration

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import org.postgresql.ds.PGPoolingDataSource


object AmazonIntegration {

  val ImageStorageName = "ndla-image"
  val ImageAPIAccessKey = "AKIAJYVWL6WV5PL3XCEQ"
  val ImageAPISecretAccessKey = "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"

  private val datasource = new PGPoolingDataSource()
  datasource.setUser("imageapi_write")
  datasource.setPassword("cx8QnLj9qEszrep")
  datasource.setDatabaseName("ndla_image_api_test")
  datasource.setServerName("ndla-image-api-test.c7wszsjus6q8.eu-central-1.rds.amazonaws.com")
  datasource.setPortNumber(5432)
  datasource.setInitialConnections(3)
  datasource.setMaxConnections(20)

  def getImageStorageDefaultCredentials(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new ProfileCredentialsProvider())
    s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    new AmazonImageStorage(ImageStorageName, s3Client)
  }

  def getImageStorage(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new BasicAWSCredentials(ImageAPIAccessKey, ImageAPISecretAccessKey))
    s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    new AmazonImageStorage(ImageStorageName, s3Client)
  }

  def getImageMeta(): DbImageMeta = {
    new DbImageMeta(datasource)
  }

}
