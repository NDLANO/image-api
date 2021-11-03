/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties._
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc._

class V7__TranslateUntranslatedAuthors extends BaseJavaMigration with LazyLogging {
  // Some contributors were not translated V6
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      imagesToUpdate.map(t => updateAuthorFormat(t._1, t._2)).foreach(update)
    }
  }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => {
        (rs.long("id"), rs.string("metadata"))
      })
      .list()
  }

  def toNewAuthorType(author: V5_Author): V5_Author = {
    val creatorMap = (oldCreatorTypes zip creatorTypes).toMap.withDefaultValue(None)
    val processorMap = (oldProcessorTypes zip processorTypes).toMap.withDefaultValue(None)
    val rightsholderMap = (oldRightsholderTypes zip rightsholderTypes).toMap.withDefaultValue(None)

    (creatorMap(author.`type`.toLowerCase),
     processorMap(author.`type`.toLowerCase),
     rightsholderMap(author.`type`.toLowerCase)) match {
      case (t: String, _, _) => V5_Author(t.capitalize, author.name)
      case (_, t: String, _) => V5_Author(t.capitalize, author.name)
      case (_, _, t: String) => V5_Author(t.capitalize, author.name)
      case (_, _, _)         => author
    }
  }

  def updateAuthorFormat(id: Long, metaString: String): V6_ImageMetaInformation = {
    val meta = read[V6_ImageMetaInformation](metaString)

    val creators = meta.copyright.creators.map(toNewAuthorType)
    val processors = meta.copyright.processors.map(toNewAuthorType)
    val rightsholders = meta.copyright.rightsholders.map(toNewAuthorType)

    meta.copy(Some(id),
              copyright =
                meta.copyright.copy(creators = creators, processors = processors, rightsholders = rightsholders))
  }

  def update(imagemetadata: V6_ImageMetaInformation)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(imagemetadata))

    sql"update imagemetadata set metadata = ${dataObject} where id = ${imagemetadata.id}".update()
  }

}
