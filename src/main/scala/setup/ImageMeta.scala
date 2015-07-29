package setup

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{Item, DynamoDB}
import com.amazonaws.services.dynamodbv2.model._
import model.Image

import scala.collection.JavaConversions._

class ImageMeta(ImageMetaName: String, ImageBucketName: String) {

  val BucketHost = "s3.amazonaws.com"
  val dynamoDb = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider()))
  val KeySchemas = List(new KeySchemaElement().withAttributeName("Id").withKeyType(KeyType.HASH))
  val AttributeDefinitions = List(new AttributeDefinition("Id", "N"))

  def upload(image: Image) = {
    val item = new Item()
      .withPrimaryKey("Id", image.id)
      .withString("Title", image.title)
      .withString("ThumbPath", createUrl(image.thumbPath))
      .withString("ImagePath", createUrl(image.imagePath))
      .withStringSet("Tags", new java.util.HashSet(image.tags))

    dynamoDb.getTable(ImageMetaName).putItem(item)
  }

  def createUrl(imagePath: String): String = {
    "https://" + ImageBucketName + "." + BucketHost + "/" + imagePath
  }

  def create() = {
    val createRequest = new CreateTableRequest()
      .withTableName(ImageMetaName)
      .withKeySchema(KeySchemas)
      .withAttributeDefinitions(AttributeDefinitions)
      .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L))

    val newTable = dynamoDb.createTable(createRequest)
    newTable.waitForActive()
  }


  def exists():Boolean = {
    try {
      Option(dynamoDb.getTable(ImageMetaName).describe()).isDefined
    } catch {
      case e:ResourceNotFoundException => false
    }
  }

}
