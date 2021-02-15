/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V9__AddEditorNotesToImages extends BaseJavaMigration {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val timeService = new TimeService()

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      imagesToUpdate.map(convertImageUpdate).foreach(update) // TODO fix this, either as in V3 or V8?
    }
  }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => {
        (rs.long("id"), rs.string("metadata"))
      })
      .list()
      .apply()
  }

  def convertImageUpdate(imageMeta: V3__DBImageMetaInformation): V3__DBImageMetaInformation = {
    val oldDocument = parse(imageMeta.document)
    val updatedByString = (oldDocument \ "updatedBy").extract[String]
    val updatedString = (oldDocument \ "updated").extract[String]

    val updatedJson = parse(
      s"""{"createdBy": "$updatedByString",
         | "created": "$updatedString",
         |  modelReleased:"not model released",
         |  EditorNotes:[]
         |  }""".stripMargin) // TODO se over den siste, og om datoen er rett som streng

    val mergedDoc = oldDocument.merge(updatedJson)
    imageMeta.copy(document = compact(render(mergedDoc)))
  }

  def update(imageMeta: V3__DBImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imageMeta.document)

    sql"update imagemetadata set metadata = $dataObject where id = ${imageMeta.id}".update().apply()
  }

/*
  TODO: Delete
  val oldDocument = parse(imageMeta.document)                            // Parser json-strengen i databasen til et json4s objekt
  val updatedByString = (oldDocument \ "updatedBy").extract[String]      // Henter ut verdien "updatedBy" fra json4s objektet og extracter det som String
  val updatedJson = parse(s"""{"createdBy": "$updatedByString"}""")      // Lager et nytt json4s objekt fra strengen med verdiene vi putter inn
  val mergedDoc = oldDocument.merge(updatedJson)                         // Slår sammen json4s objektene (om felter finnes i begge så "vinner" de i updatedJson
  imageMeta.copy(document = compact(render(mergedDoc)))                  // Lag et nytt objekt av samme type som imageMeta med samme verdier med unntak av document
*/// Editor Notes = Liste med {timestamp, user, note}

}

case class V9__EditorNote(note: String, user: String, timestamp: Date)
