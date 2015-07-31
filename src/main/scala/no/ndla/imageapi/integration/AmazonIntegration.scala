package no.ndla.imageapi.integration

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3Client


object AmazonIntegration {

  val ImageBucketName = "ndla-image"
  val ImageMetaName = "ndla-image-meta"

  def getImageMeta(): AmazonImageMeta = {
    val dbClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider())
    val dynamoDb = new DynamoDB(dbClient)
    new AmazonImageMeta(ImageMetaName, dbClient, dynamoDb)
  }

  def getImageBucket(): AmazonImageBucket = {
    val s3Client = new AmazonS3Client(new ProfileCredentialsProvider())
    s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    new AmazonImageBucket(ImageBucketName, s3Client)
  }

}
