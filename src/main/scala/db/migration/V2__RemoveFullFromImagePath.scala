/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package db.migration

import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.postgresql.util.PGobject
import scalikejdbc._

class V2__RemoveFullFromImagePath extends BaseJavaMigration with LazyLogging {

  implicit val formats = org.json4s.DefaultFormats

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)
    logger.info("Starting V2__RemoveFullFromImagePath DB Migration")
    val dBstartMillis = System.currentTimeMillis()

    db.withinTx { implicit session =>
      allImages.map(convertImageUrl).foreach(update)
    }
    logger.info(s"Done V2__RemoveFullFromImagePath DB Migration tok ${System.currentTimeMillis() - dBstartMillis} ms")

  }

  def allImages(implicit session: DBSession): List[V2_DBImageMetaInformation] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => V2_DBImageMetaInformation(rs.long("id"), rs.string("metadata")))
      .list()
  }

  def convertImageUrl(imageMeta: V2_DBImageMetaInformation): V2_DBImageMetaInformation = {
    val oldDocument = parse(imageMeta.document)

    val updatedDocument = oldDocument mapField {
      case ("imageUrl", JString(oldUrl)) => ("imageUrl", JString(oldUrl.replace("full/", "")))
      case x                             => x
    }
    imageMeta.copy(document = compact(render(updatedDocument)))
  }

  def update(imageMeta: V2_DBImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update()
  }
}

case class V2_DBImageMetaInformation(id: Long, document: String)
