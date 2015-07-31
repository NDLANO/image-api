package no.ndla.imageapi.integration

import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.model._
import model.Image
import no.ndla.imageapi.business.ImageMeta

import scala.collection.JavaConversions._

class AmazonImageMeta(imageMetaName:String, dbClient: AmazonDynamoDBClient, dynamoDb: DynamoDB) extends ImageMeta {

  val ScanLimit = 100

  //TODO: Gir kun 100 treff, men scan støtter paging, så vi kan tilby det kanskje.
  def all(): List[Image] = {
    dbClient.scan(new ScanRequest(imageMetaName).
      withLimit(ScanLimit))
      .getItems
      .map(toImageFromMap(_))
      .toList.sortBy(_.id)
  }

  def withId(id: String): Option[Image] = {
    Option(dynamoDb.getTable(imageMetaName).getItem("Id", id)) match {
      case Some(item) => Option(toImageFromItem(item))
      case None => None
    }
  }

  //TODO: Funker bare for en enkelt tag (ikke hverken tags=elg,jerv eller tags=elg&tags=jerv)
  //TODO: Scan har performance issues (gjør en table-scan alltid), vurder en annen datamodell for søk
  def withTags(tag: String): List[Image] = {
    val condition = new Condition()
      .withComparisonOperator(ComparisonOperator.CONTAINS)
      .withAttributeValueList(new AttributeValue().withS(tag))

    val scanRequest = new ScanRequest(imageMetaName).withLimit(ScanLimit)
    scanRequest.addScanFilterEntry("Tags", condition)

    dbClient.scan(scanRequest).getItems.map(toImageFromMap(_)).toList.sortBy(_.id)
  }

  def upload(image: Image) = {
    val item = new Item()
      .withPrimaryKey("Id", image.id)
      .withString("Title", image.title)
      .withString("ThumbPath", image.thumbPath)
      .withString("ImagePath", image.imagePath)
      .withStringSet("Tags", new java.util.HashSet(image.tags))

    dynamoDb.getTable(imageMetaName).putItem(item)
  }

  def exists():Boolean = {
    try {
      Option(dynamoDb.getTable(imageMetaName).describe()).isDefined
    } catch {
      case e:ResourceNotFoundException => false
    }
  }

  def create() = {
    val KeySchemas = List(new KeySchemaElement().withAttributeName("Id").withKeyType(KeyType.HASH))
    val AttributeDefinitions = List(new AttributeDefinition("Id", "S"))

    val createRequest = new CreateTableRequest()
      .withTableName(imageMetaName)
      .withKeySchema(KeySchemas)
      .withAttributeDefinitions(AttributeDefinitions)
      .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L))

    val newTable = dynamoDb.createTable(createRequest)
    newTable.waitForActive()
  }

  // TODO: Gjør noe for å unngå to nesten helt klin like metoder
  private def toImageFromItem(item: Item): Image = {
    Image(item.getString("Id"),
      item.getString("Title"),
      item.getString("ThumbPath"),
      item.getString("ImagePath"),
      item.getList("Tags").toList)
  }

  // TODO: Gjør noe for å unngå to nesten helt klin like metoder
  private def toImageFromMap(mapEntry: util.Map[String, AttributeValue]): Image = {
    Image(mapEntry.get("Id").getS(),
      mapEntry.get("Title").getS(),
      mapEntry.get("ThumbPath").getS(),
      mapEntry.get("ImagePath").getS(),
      mapEntry.get("Tags").getSS().toList)
  }
}
