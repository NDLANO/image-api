/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import io.lemonlabs.uri.Uri
import no.ndla.imageapi.ComponentRegistry.{amazonClient, imageStorage}

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.{JField, JString}
import org.json4s.{DefaultFormats, JArray, JLong, JObject}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import scala.util.{Failure, Success, Try}

class V12__AddImageMetadata extends BaseJavaMigration {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val timeService = new TimeService()

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      imagesToUpdate.map {
        case (id, document) => update(convertImageUpdate(document), id)
      }
    }
  }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => {
        (rs.long("id"), rs.string("metadata"))
      })
      .list()
  }


  def convertImageUpdate(imageMeta: String): String = {
    val oldDocument = parse(imageMeta)

    val imageUrl = (oldDocument \ "imageUrl").extract[String]
    val imageKey = Uri
      .parse(imageUrl)
      .toStringRaw
      .dropWhile(_ == '/') // Strip heading '/'
    val mergeObject = imageStorage.get(imageKey) match {
      case Success(value) => {
        val image = value.sourceImage
        JObject(
          JField("width", JLong(image.getWidth())),
          JField("height", JLong(image.getHeight()))
        )
      }
      case Failure(_) => JObject()
    }

    val mergedDoc = oldDocument.merge(mergeObject)
    compact(render(mergedDoc))
  }

  def update(imagemetadata: String, id: Long)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)

    sql"update imagemetadata set metadata = ${dataObject} where id = $id".update()
  }
}
