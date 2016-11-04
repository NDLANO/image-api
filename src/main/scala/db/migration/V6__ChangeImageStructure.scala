package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc.{DBSession, DB, _}
import org.json4s.JsonDSL._



class V6__ChangeImageStructure extends JdbcMigration{
  implicit val formats = org.json4s.DefaultFormats

  implicit class JValueExtended(value: JValue) {
    def has(childString: String): Boolean = (value \ childString) != JNothing
  }
  override def migrate(connection: Connection): Unit  = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages.flatMap(removeImageVariants).map(update)
    }
  }

  def allImages(implicit session: DBSession): List[V6_ImageJson] = {
    sql"select id, metadata from imagemetadata".map(rs => V6_ImageJson(rs.long("id"), rs.string("metadata"))).list().apply()
  }

  def removeImageVariants(imageJson: V6_ImageJson) : Option[V6_ImageJson]  = {
    val json = parse(imageJson.metadata)
    val imageBlock = json \\ "images"

    val hasImages = json.has("images")

    val removedImages = json.removeField {
      case ("images", _) => true
      case _ => false
    }

    {"tags": [{"tags": ["oslo", "etnisitet", "etnisk", "grønland", "mat"], "language": "nn"}, {"tags": ["oslo", "etnisitet", "etnisk", "grønland", "indisk", "indisk mat", "mat", "restaurant"], "language": "nb"}, {"tags": ["oslo", "ethnicity", "ethnic", "greenland", "indian", "indian food", "food", "restaurant"]}, {"tags": ["oslo", "ethnicity", "ethnic", "greenland", "indian", "indian food", "food", "restaurant"], "language": "en"}], "images": {"full": {"url": "full/Indisk mat på Grønland_1.jpg", "size": 123989, "contentType": "image/jpeg"}, "small": {"url": "thumbs/Indisk mat på Grønland_1.jpg", "size": 7302, "contentType": "image/jpeg"}}, "titles": [{"title": "Indisk mat på Grønland, Oslo"}], "alttexts": [{"alttext": "Indisk mat på Grønland, Oslo. Foto."}], "captions": [], "copyright": {"origin": "http://www.scanpix.no", "authors": [{"name": "Joronn Sagen Engen", "type": "Fotograf"}, {"name": "Aftenposten", "type": "Leverandør"}, {"name": "NTB scanpix", "type": "Leverandør"}], "license": {"url": "https://creativecommons.org/licenses/by-nc-sa/2.0/", "license": "by-nc-sa", "description": "Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic"}}}

    hasImages match {
      case false => None
      case true => {
        val imageBlockOptop: Option[V6_ImageVariants] = imageBlock.extractOpt[V6_ImageVariants]
        val oldImageBlock = imageBlockOptop.get.full.get
        val updatedImage = removedImages.merge(
          ("imageUrl", oldImageBlock.url) ~
            ("size", oldImageBlock.size) ~
            ("contentType", oldImageBlock.contentType)
        )
        Some(imageJson.copy(metadata = compact(render(parse(write(updatedImage))))))
      }
    }
  }
  def update(image: V6_ImageJson)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(image.metadata)

    sql"update imagemetadata set metadata = $dataObject where id = ${image.id}".update().apply
  }
}

case class V6_ImageVariants(small: Option[V6_Image], full: Option[V6_Image])
case class V6_Image(url: String, size: Int, contentType: String)
case class V6_ImageJson(id: Long, metadata: String)
