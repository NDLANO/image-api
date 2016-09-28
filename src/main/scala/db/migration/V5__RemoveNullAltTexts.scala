/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V5__RemoveNullAltTexts extends JdbcMigration {

  implicit val formats = org.json4s.DefaultFormats + V5_ImageMetaInformation.JSonSerializer

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages
        .filter(_.alttexts.exists(_.alttext == null))
        .foreach(img => {
          update(img.copy(alttexts = img.alttexts.filterNot(_.alttext == null)))
        })
    }
  }

  def allImages(implicit session: DBSession): List[V5_ImageMetaInformation] = {
    sql"select id, metadata from imagemetadata".map(rs => {
      read[V5_ImageMetaInformation](rs.string("metadata")).copy(id = Some(rs.long("id")))
    }).list().apply()
  }

  def update(image: V5_ImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(image))

    sql"update imagemetadata set metadata = $dataObject where id = ${image.id.get}".update().apply
  }
}

case class V5_ImageTitle(title: String, language: Option[String])
case class V5_ImageAltText(alttext: String, language: Option[String])
case class V5_ImageCaption(caption: String, language: Option[String])
case class V5_ImageTag(tags: Seq[String], language: Option[String])
case class V5_ImageVariants(small: Option[V5_Image], full: Option[V5_Image])
case class V5_Image(url: String, size: Int, contentType: String)
case class V5_Copyright(license: V5_License, origin: String, authors: List[V5_Author])
case class V5_License(license: String, description: String, url: Option[String])
case class V5_Author(`type`: String, name: String)
case class V5_ImageMetaInformation(id: Option[Long], titles: Seq[V5_ImageTitle], alttexts: Seq[V5_ImageAltText], images: V5_ImageVariants, copyright: V5_Copyright, tags: Seq[V5_ImageTag], captions: Seq[V5_ImageCaption])

object V5_ImageMetaInformation {
  val JSonSerializer = FieldSerializer[V5_ImageMetaInformation](ignore("id"))
}
