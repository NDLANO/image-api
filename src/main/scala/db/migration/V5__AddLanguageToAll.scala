/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection
import java.util.Date

import no.ndla.imageapi.model.Language
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V5__AddLanguageToAll extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats + FieldSerializer[V5_ImageMetaInformation](ignore("id"))

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages.map(updateImageLanguage).foreach(update)
    }
  }

  def updateImageLanguage(audioMeta: V5_ImageMetaInformation): V5_ImageMetaInformation = {
    audioMeta.copy(
      titles = audioMeta.titles.map(t => V5_ImageTitle(t.title, Some(Language.languageOrUnknown(t.language)))),
      alttexts = audioMeta.alttexts.map(t => V5_ImageAltText(t.alttext, Some(Language.languageOrUnknown(t.language)))),
      tags = audioMeta.tags.map(t => V5_ImageTag(t.tags, Some(Language.languageOrUnknown(t.language)))),
      captions = audioMeta.captions.map(t => V5_ImageCaption(t.caption, Some(Language.languageOrUnknown(t.language))))
    )
  }

  def allImages(implicit session: DBSession): List[V5_ImageMetaInformation] = {
    sql"select id, metadata from imagemetadata".map(rs => {
      val meta = read[V5_ImageMetaInformation](rs.string("metadata"))
      V5_ImageMetaInformation(
        Some(rs.long("id")),
        meta.titles,
        meta.alttexts,
        meta.imageUrl,
        meta.size,
        meta.contentType,
        meta.copyright,
        meta.tags,
        meta.captions,
        meta.updatedBy,
        meta.updated)
    }
    ).list().apply()
  }

  def update(imagemetadata: V5_ImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(imagemetadata))

    sql"update imagemetadata set metadata = $dataObject where id = ${imagemetadata.id}".update().apply
  }

}

case class V5_ImageTitle(title: String, language: Option[String])
case class V5_ImageAltText(alttext: String, language: Option[String])
case class V5_ImageCaption(caption: String, language: Option[String])
case class V5_ImageTag(tags: Seq[String], language: Option[String])
case class V5_Image(fileName: String, size: Long, contentType: String)
case class V5_Copyright(license: V5_License, origin: String, authors: Seq[V5_Author])
case class V5_License(license: String, description: String, url: Option[String])
case class V5_Author(`type`: String, name: String)
case class V5_ImageMetaInformation(id: Option[Long],
                                 titles: Seq[V5_ImageTitle],
                                 alttexts: Seq[V5_ImageAltText],
                                 imageUrl: String,
                                 size: Long,
                                 contentType: String,
                                 copyright: V5_Copyright,
                                 tags: Seq[V5_ImageTag],
                                 captions: Seq[V5_ImageCaption],
                                 updatedBy: String,
                                 updated: Date)