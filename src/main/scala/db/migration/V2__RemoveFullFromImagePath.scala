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
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

class V2__RemoveFullFromImagePath extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages.map(convertImageUrl).foreach(update)
    }
  }

  def allImages(implicit session: DBSession): List[V2_DBImageMetaInformation] = {
    sql"select id, metadata from imagemetadata".map(rs => V2_DBImageMetaInformation(rs.long("id"), rs.string("metadata"))).list().apply()
  }

  def convertImageUrl(imageMeta: V2_DBImageMetaInformation): V2_DBImageMetaInformation = {
    val oldDocument = parse(imageMeta.document)

    val updatedDocument = oldDocument mapField {
      case ("imageUrl", JString(oldUrl)) => ("imageUrl", JString(oldUrl.replace("full/", "")))
      case x => x
    }
    imageMeta.copy(document = compact(render(updatedDocument)))
  }

  def update(imageMeta: V2_DBImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update().apply
  }
}

case class V2_DBImageMetaInformation(id: Long, document: String)
