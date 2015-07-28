package setup

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.model._
import model.ImageData

import scala.collection.JavaConversions._

object DynamoBootstrap {
  val ImageMetaTableName = "image-meta"

  val KeySchemas = List(new KeySchemaElement().withAttributeName("Id").withKeyType(KeyType.HASH))
  val AttributeDefinitions = List(new AttributeDefinition("Id", "N"))

  def main(args: Array[String]) {
    val dynamoDb = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider()))
    if(!tableExists(dynamoDb)) {
      createTable(dynamoDb)
      ImageData.all.foreach(image => loadImage(dynamoDb, image))
    }
  }

  def tableExists(dynamoDb: DynamoDB): Boolean = {
    try {
      Option(dynamoDb.getTable(ImageMetaTableName).describe()).isDefined
    } catch {
      case e:ResourceNotFoundException => false
    }
  }

  def createTable(dynamoDb: DynamoDB): Unit = {
    val createRequest = new CreateTableRequest()
      .withTableName(ImageMetaTableName)
      .withKeySchema(KeySchemas)
      .withAttributeDefinitions(AttributeDefinitions)
      .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L))

    val newTable = dynamoDb.createTable(createRequest)
    newTable.waitForActive()
  }

  def loadImage(dynamoDb: DynamoDB, image: model.Image) = {
    val item = new Item()
      .withPrimaryKey("Id", image.id)
      .withString("thumbPath", image.thumbPath)
      .withString("imagePath", image.imagePath)
      .withStringSet("tags", new java.util.HashSet(image.tags))

    dynamoDb.getTable(ImageMetaTableName).putItem(item)
  }

}
