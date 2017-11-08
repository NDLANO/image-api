/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection
import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties._
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc._

class V6__AuthorFormatUpdated extends JdbcMigration with LazyLogging  {
  // Authors are now split into three categories `creators`, `processors` and `rightsholders`
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      imagesToUpdate.map(t => updateAuthorFormat(t._1, t._2)).foreach(update)
    }
  }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata".map(rs => {(rs.long("id"),rs.string("metadata"))
    }).list().apply()
  }

  def toNewAuthorType(author: V5_Author): V5_Author = {
    val creatorMap = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
    val processorMap = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
    val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

    (creatorMap(author.`type`.toLowerCase), processorMap(author.`type`.toLowerCase), rightsholderMap(author.`type`.toLowerCase)) match {
      case (t: String, None, None) => V5_Author(t.capitalize, author.name)
      case (None, t: String, None) => V5_Author(t.capitalize, author.name)
      case (None, None, t: String) => V5_Author(t.capitalize, author.name)
      case (_, _, _) => V5_Author(author.`type`, author.name)
    }
  }

  def updateAuthorFormat(id: Long, metaString: String): V6_ImageMetaInformation = {
    val meta = read[V5_ImageMetaInformation](metaString)
    val metaV6 = read[V6_ImageMetaInformation](metaString)

    // If entry contains V6 features -> Don't update.
    if(metaV6.copyright.creators.nonEmpty ||
      metaV6.copyright.processors.nonEmpty ||
      metaV6.copyright.rightsholders.nonEmpty ||
      metaV6.copyright.validFrom.nonEmpty ||
      metaV6.copyright.validTo.nonEmpty
    ) {
      metaV6.copy(id = None)
    } else {
      val creators = meta.copyright.authors.filter(a => oldCreatorTypes.contains(a.`type`.toLowerCase)).map(toNewAuthorType)
      // Filters out processor authors with old type `redaksjonelt` during import process since `redaksjonelt` exists both in processors and creators.
      val processors = meta.copyright.authors.filter(a => oldProcessorTypes.contains(a.`type`.toLowerCase)).filterNot(a => a.`type`.toLowerCase == "redaksjonelt").map(toNewAuthorType)
      val rightsholders = meta.copyright.authors.filter(a => oldRightsholderTypes.contains(a.`type`.toLowerCase)).map(toNewAuthorType)

      V6_ImageMetaInformation(
        Some(id),
        meta.titles,
        meta.alttexts,
        meta.imageUrl,
        meta.size,
        meta.contentType,
        V6_Copyright(meta.copyright.license, meta.copyright.origin, creators, processors, rightsholders, None, None),
        meta.tags,
        meta.captions,
        meta.updatedBy,
        meta.updated
      )
    }
  }

  def update(imagemetadata: V6_ImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(imagemetadata))

    sql"update imagemetadata set metadata = $dataObject where id = ${imagemetadata.id}".update().apply
  }

}

case class V6_Copyright(license: V5_License, origin: String, creators: Seq[V5_Author], processors: Seq[V5_Author], rightsholders: Seq[V5_Author], validFrom: Option[Date], validTo: Option[Date])
case class V6_ImageMetaInformation(id: Option[Long],
                                   titles: Seq[V5_ImageTitle],
                                   alttexts: Seq[V5_ImageAltText],
                                   imageUrl: String,
                                   size: Long,
                                   contentType: String,
                                   copyright: V6_Copyright,
                                   tags: Seq[V5_ImageTag],
                                   captions: Seq[V5_ImageCaption],
                                   updatedBy: String,
                                   updated: Date)
