package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V4__AddCaptionsList extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages.flatMap(addCaptionsArray).foreach(update)
    }
  }

  def allImages(implicit session: DBSession): List[V4_DBImage] = {
    sql"select id, metadata from imagemetadata".map(rs => V4_DBImage(rs.long("id"), rs.string("metadata"))).list().apply()
  }

  def addCaptionsArray(image: V4_DBImage): Option[V4_DBImage] = {
    val json = parse(image.metadata)
    val captions = json \\ "captions"

    captions.extractOpt[Seq[V4_ImageCaption]] match {
      case None => {
        val imageWithCaptions = json merge parse(write(V4_Image(Seq())))
        Some(image.copy(metadata = compact(render(imageWithCaptions))))
      }
      case Some(caption) => None
    }
  }

  def update(image: V4_DBImage)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(image.metadata)

    sql"update imagemetadata set metadata = $dataObject where id = ${image.id}".update().apply
  }
}

case class V4_DBImage(id: Long, metadata: String)
case class V4_Image(captions: Seq[V4_ImageCaption])
case class V4_ImageCaption(caption: String, language: Option[String])
