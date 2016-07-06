package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V3__RenameTagToTags extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.flatMap(convertTagsToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V3_DBImage] = {
    sql"select id, metadata from imagemetadata".map(rs => V3_DBImage(rs.long("id"), rs.string("metadata"))).list().apply()
  }

  def convertTagsToNewFormat(image: V3_DBImage): Option[V3_DBImage] = {
    val json = parse(image.metadata)
    val tag = json \\ "tags"

    val oldTagsOpt: Option[List[V3_OldTags]] = tag.extractOpt[List[V3_OldTags]]
    oldTagsOpt match {
      case None => None
      case Some(oldTags) => {
        val newTags = oldTags.map(oldTag => V3_NewTags(oldTag.tag, oldTag.language))
        Some(image.copy(metadata = compact(render(json.replace(List("tags"), parse(write(newTags)))))))
      }
    }
  }

  def update(image: V3_DBImage)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(image.metadata)

    sql"update imagemetadata set metadata = $dataObject where id = ${image.id}".update().apply
  }
}

case class V3_NewTags(tags: Seq[String], language:Option[String])
case class V3_OldTags(tag: Seq[String], language:Option[String])
case class V3_DBImage(id: Long, metadata: String)
