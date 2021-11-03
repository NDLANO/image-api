/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

class V3__AddUpdatedColoums extends BaseJavaMigration with LazyLogging {

  implicit val formats = org.json4s.DefaultFormats
  val timeService = new TimeService()

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)
    logger.info("Starting V3__AddUpdatedColoums DB Migration")
    val dBstartMillis = System.currentTimeMillis()

    db.withinTx { implicit session =>
      allImages.map(convertImageUpdate).foreach(update)
    }
    logger.info(s"Done V3__AddUpdatedColoums DB Migration tok ${System.currentTimeMillis() - dBstartMillis} ms")
  }

  def allImages(implicit session: DBSession): List[V3__DBImageMetaInformation] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => V3__DBImageMetaInformation(rs.long("id"), rs.string("metadata")))
      .list()
  }

  def convertImageUpdate(imageMeta: V3__DBImageMetaInformation): V3__DBImageMetaInformation = {
    val oldDocument = parse(imageMeta.document)
    val updatedJson = parse(s"""{"updatedBy": "content-import-client", "updated": "${timeService.nowAsString()}"}""")

    val mergedDoc = oldDocument merge updatedJson

    imageMeta.copy(document = compact(render(mergedDoc)))
  }

  def update(imageMeta: V3__DBImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update()
  }

}

case class V3__DBImageMetaInformation(id: Long, document: String)

class TimeService() {

  def nowAsString(): String = {
    //NB!!! BUG day format is wrong should have been dd, and the Z should have been 'Z'
    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-DD'T'HH:mm:ssZ")
    (new DateTime).toString(formatter)
  }
}
