/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._


class V3_AddUpdatedColoums extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats
  val timeService = new TimeService()

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages.map(convertImageUpdate).foreach(update)
    }
  }

  def allImages(implicit session: DBSession): List[V3_DBImageMetaInformation] = {
    sql"select id, metadata from imagemetadata".map(rs => V3_DBImageMetaInformation(rs.long("id"), rs.string("metadata"))).list().apply()
  }

  def convertImageUpdate(imageMeta: V3_DBImageMetaInformation): V3_DBImageMetaInformation = {

    val oldDocument = parse(imageMeta.document)

    val updatedJson = parse(s"""{"updatedBy": "content-import-client", "updated": "${timeService.nowAsString()}"}""")

    val mergedDoc = oldDocument merge updatedJson

    imageMeta.copy(document = compact(render(mergedDoc)))
  }

  def update(imageMeta: V3_DBImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update().apply
  }

}

case class V3_DBImageMetaInformation(id: Long, document: String)

class TimeService() {
  def nowAsString(): String = {
    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-DD'T'HH:mm:ssZ")
    (new DateTime).toString(formatter)
  }
}
