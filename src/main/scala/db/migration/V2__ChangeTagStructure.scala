/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc._

class V2__ChangeTagStructure extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.flatMap(convertTagsToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V2_DBImage] = {
    sql"select id, metadata from imagemetadata".map(rs => V2_DBImage(rs.long("id"), rs.string("metadata"))).list().apply()
  }

  def convertTagsToNewFormat(image: V2_DBImage): Option[V2_DBImage] = {
    val json = parse(image.metadata)
    val tags = json \\ "tags"

    val oldTagsOpt: Option[List[V2_OldTag]] = tags.extractOpt[List[V2_OldTag]]
    oldTagsOpt match {
      case None => None
      case Some(oldTags) => {
        val newTags = oldTags.groupBy(_.language).map(entry => (entry._1, entry._2.map(_.tag))).map(entr => V2_ImageTags(entr._2, entr._1))
        Some(image.copy(metadata = compact(render(json.replace(List("tags"), parse(write(newTags)))))))
      }
    }
  }

  def update(image: V2_DBImage)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(image.metadata)

    sql"update imagemetadata set metadata = $dataObject where id = ${image.id}".update().apply
  }
}

case class V2_ImageTags(tag: Seq[String], language:Option[String])
case class V2_OldTag(tag: String, language: Option[String])
case class V2_DBImage(id: Long, metadata: String)
