package no.ndla.imageapi.integration

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3Client


object AmazonIntegration {

  val ImageStorageName = "ndla-image"
  val ImageAPIAccessKey = "AKIAJYVWL6WV5PL3XCEQ"
  val ImageAPISecretAccessKey = "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"

  def getImageStorage(): AmazonImageStorage = {
    val s3Client = new AmazonS3Client(new BasicAWSCredentials(ImageAPIAccessKey, ImageAPISecretAccessKey))
    s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    new AmazonImageStorage(ImageStorageName, s3Client)
  }

  def getImageMeta(): DbImageMeta = {
    new DbImageMeta
  }
}
