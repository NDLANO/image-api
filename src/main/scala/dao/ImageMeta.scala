package dao

import java.util

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.document.{PrimaryKey, DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.model._
import model.Image

import scala.collection.JavaConversions._

class ImageMeta(ImageMetaName: String = "ndla-image-meta", ImageBucketName: String = "ndla-image") {

  val BucketHost = "s3.amazonaws.com"
  val ScanLimit = 100

  //TODO: Må ha en egen bruker for applikasjonen. Hvordan håndtere credentials der? Som konfigurasjon?
  val dbClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider())
  val dynamoDb = new DynamoDB(dbClient)

  //TODO: Gir kun 100 treff, men scan støtter paging, så vi kan tilby det kanskje.
  def all(): List[Image] = {
    dbClient.scan(new ScanRequest(ImageMetaName).
      withLimit(ScanLimit))
      .getItems
      .map(toImage(_))
      .toList.sortBy(_.id)
  }

  def withId(id: String): Option[Image] = {
    Option(dynamoDb.getTable(ImageMetaName).getItem("Id", id)) match {
      case Some(item) => Option(toImage2(item))
      case None => None
    }
  }

  //TODO: Funker bare for en enkelt tag (ikke hverken tags=elg,jerv eller tags=elg&tags=jerv)
  //TODO: Scan har performance issues (gjør en table-scan alltid), vurder en annen datamodell for søk
  def withTags(tag: String): List[Image] = {
    val condition = new Condition()
      .withComparisonOperator(ComparisonOperator.CONTAINS)
      .withAttributeValueList(new AttributeValue().withS(tag))

    val scanRequest = new ScanRequest(ImageMetaName).withLimit(ScanLimit)
    scanRequest.addScanFilterEntry("Tags", condition)

    dbClient.scan(scanRequest).getItems.map(toImage(_)).toList.sortBy(_.id)
  }

  // TODO: Gjør noe for å unngå to nesten helt klin like metoder
  def toImage2(item: Item): Image = {
    Image(item.getString("Id"),
      item.getString("Title"),
      item.getString("ThumbPath"),
      item.getString("ImagePath"),
      item.getList("Tags").toList)
  }

  // TODO: Gjør noe for å unngå to nesten helt klin like metoder
  def toImage(mapEntry: util.Map[String, AttributeValue]): Image = {
    Image(mapEntry.get("Id").getS(),
      mapEntry.get("Title").getS(),
      mapEntry.get("ThumbPath").getS(),
      mapEntry.get("ImagePath").getS(),
      mapEntry.get("Tags").getSS().toList)
  }

  def upload(image: Image) = {
    val item = new Item()
      .withPrimaryKey("Id", image.id)
      .withString("Title", image.title)
      .withString("ThumbPath", image.thumbPath)
      .withString("ImagePath", image.imagePath)
      .withStringSet("Tags", new java.util.HashSet(image.tags))

    dynamoDb.getTable(ImageMetaName).putItem(item)
  }

  def create() = {
    val KeySchemas = List(new KeySchemaElement().withAttributeName("Id").withKeyType(KeyType.HASH))
    val AttributeDefinitions = List(new AttributeDefinition("Id", "S"))

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
