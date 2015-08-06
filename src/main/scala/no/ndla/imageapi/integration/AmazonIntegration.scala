package no.ndla.imageapi.integration

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3Client


object AmazonIntegration {

  val ImageStorageName = "ndla-image"
  val ImageMetaName = "ndla-image-meta"

  val ImageAPIAccessKey = "AKIAJYVWL6WV5PL3XCEQ"
  val ImageAPISecretAccessKey = "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"

  def getImageMetaWithDefaultCredentials(tableName:String = ImageMetaName): AmazonImageMeta = {
    val dbClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider())
    val dynamoDb = new DynamoDB(dbClient)
    new AmazonImageMeta(tableName, dbClient, dynamoDb)
  }

  def getImageMeta(tableName:String = ImageMetaName): AmazonImageMeta = {
    val dbClient = new AmazonDynamoDBClient(new BasicAWSCredentials(ImageAPIAccessKey, ImageAPISecretAccessKey))
    val dynamoDb = new DynamoDB(dbClient)
    new AmazonImageMeta(tableName, dbClient, dynamoDb)
  }

  def getImageStorage(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new BasicAWSCredentials(ImageAPIAccessKey, ImageAPISecretAccessKey))
    s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    new AmazonImageStorage(ImageStorageName, s3Client)
  }

}
