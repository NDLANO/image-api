package no.ndla.imageapi.integration

import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.model._
import model._
import no.ndla.imageapi.business.ImageMeta

import scala.collection.JavaConversions._

class AmazonImageMeta(imageMetaName:String, dbClient: AmazonDynamoDBClient, dynamoDb: DynamoDB) extends ImageMeta {

  val ScanLimit = 100

  //TODO: Gir kun 100 treff, men scan støtter paging, så vi kan tilby det kanskje.
  def all(): List[ImageMetaInformation] = {
    dbClient.scan(new ScanRequest(imageMetaName).
      withLimit(ScanLimit))
      .getItems
      .map(toImageFromMap(_))
      .toList.sortBy(_.id)
  }

  def withId(id: String): Option[ImageMetaInformation] = {
    Option(dynamoDb.getTable(imageMetaName).getItem("Id", id)) match {
      case Some(item) => Option(toImageFromItem(item))
      case None => None
    }
  }

  //TODO: Funker bare for en enkelt tag (ikke hverken tags=elg,jerv eller tags=elg&tags=jerv)
  //TODO: Scan har performance issues (gjør en table-scan alltid), vurder en annen datamodell for søk
  override def withTags(tags: Iterable[String]): Iterable[ImageMetaInformation] = {
    val condition = new Condition()
      .withComparisonOperator(ComparisonOperator.CONTAINS)
      .withAttributeValueList(new AttributeValue().withS(tags.head))

    val scanRequest = new ScanRequest(imageMetaName).withLimit(ScanLimit)
    scanRequest.addScanFilterEntry("Tags", condition)

    dbClient.scan(scanRequest).getItems.map(toImageFromMap(_)).toList.sortBy(_.id)
  }

  override def containsExternalId(externalId: String): Boolean = {
    false
  }

  def upload(imageMetaInformation: ImageMetaInformation, externalId: String) = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val item = new Item()
          .withPrimaryKey("Id", imageMetaInformation.id)
          .withString("Title", imageMetaInformation.title)
          .withString("Images", write(imageMetaInformation.images))
          .withString("Copyright", write(imageMetaInformation.copyright))
          .withStringSet("Tags", new java.util.HashSet(imageMetaInformation.tags))

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

  private def toImageFromItem(item: Item): ImageMetaInformation = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    ImageMetaInformation(
      item.getString("Id"),
      item.getString("Title"),
      read[ImageVariants](item.getString("Images")),
      read[Copyright](item.getString("Copyright")),
      item.getList("Tags").toList)
  }

  private def toImageFromMap(mapEntry: util.Map[String, AttributeValue]): ImageMetaInformation = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    ImageMetaInformation(
      mapEntry.get("Id").getS(),
      mapEntry.get("Title").getS(),
      read[ImageVariants](mapEntry.get("Images").getS()),
      read[Copyright](mapEntry.get("Copyright").getS()),
      mapEntry.get("Tags").getSS().toList)
  }
}
