/*
 * Part of NDLA image-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JString
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JObject}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V8__UpdateLicenses extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      imagesToUpdate.map {
        case (id, document) => update(convertDocument(document), id)
      }
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

  def updateLicense(license: String): String = {
    val mapping = Map(
      "by" -> "CC-BY-4.0",
      "by-sa" -> "CC-BY-SA-4.0",
      "by-nc" -> "CC-BY-NC-4.0",
      "by-nd" -> "CC-BY-ND-4.0",
      "by-nc-sa" -> "CC-BY-NC-SA-4.0",
      "by-nc-nd" -> "CC-BY-NC-ND-4.0",
      "by-3.0" -> "CC-BY-4.0",
      "by-sa-3.0" -> "CC-BY-SA-4.0",
      "by-nc-3.0" -> "CC-BY-NC-4.0",
      "by-nd-3.0" -> "CC-BY-ND-4.0",
      "by-nc-sa-3.0" -> "CC-BY-NC-SA-4.0",
      "by-nc-nd-3.0" -> "CC-BY-NC-ND-4.0",
      "cc0" -> "CC0-1.0",
      "pd" -> "PD",
      "copyrighted" -> "COPYRIGHTED"
    )

    mapping.getOrElse(license, license)
  }

  def convertDocument(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("copyright", copyright: JObject) =>
        "copyright" -> copyright.mapField {
          case ("license", license: JObject) =>
            val licenseId = (license \\ "license").extract[String]
            "license" -> JString(updateLicense(licenseId))
          case x => x
        }
      case x => x
    }
    compact(render(newArticle))
  }

  def update(imagemetadata: String, id: Long)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)

    sql"update imagemetadata set metadata = ${dataObject} where id = $id".update().apply
  }

}
