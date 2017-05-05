/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

class V4__DateFormatUpdated extends JdbcMigration {
  //There was a bug in the dateformat of V3__AddUpdatedColoums had days as DD they should have been dd
  implicit val formats = org.json4s.DefaultFormats
  val timeService = new TimeService2()

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages.map(fixImageUpdatestring).foreach(update)
    }
  }

  def allImages(implicit session: DBSession): List[V4__DBImageMetaInformation] = {
    sql"select id, metadata from imagemetadata".map(rs => V4__DBImageMetaInformation(rs.long("id"), rs.string("metadata"))).list().apply()
  }

  def fixImageUpdatestring(imageMeta: V4__DBImageMetaInformation): V4__DBImageMetaInformation = {
    val oldDocument = parse(imageMeta.document)

    val updatedDocument = oldDocument mapField {
      case ("updated", JString(oldUpdated)) => ("updated", JString(timeService.nowAsString()))
      case x => x
    }

    imageMeta.copy(document = compact(render(updatedDocument)))
  }

  def update(imageMeta: V4__DBImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update().apply
  }

}

case class V4__DBImageMetaInformation(id: Long, document: String)

class TimeService2() {
  def nowAsString(): String = {
    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ssZ")
    val now = (new DateTime).toString(formatter)
    now
  }
}
