/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.integration

import javax.sql.DataSource

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.business.ImageMeta
import no.ndla.imageapi.model.{Image, ImageMetaInformation, ImageVariants}
import org.postgresql.util.PGobject
import scalikejdbc._


class PostgresMeta(dataSource: DataSource) extends ImageMeta with LazyLogging {

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override def withId(id: String): Option[ImageMetaInformation] = {
    DB readOnly {implicit session =>
      sql"select metadata from imagemetadata where id = ${id.toInt}".map(rs => rs.string("metadata")).single.apply match {
        case Some(json) => Option(asImageMetaInformation(id, json))
        case None => None
      }
    }
  }

  override def insert(imageMetaInformation: ImageMetaInformation, externalId: String): Unit = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val json = write(imageMetaInformation)

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)

    DB localTx {implicit session =>
      sql"insert into imagemetadata(external_id, metadata) values(${externalId}, ${dataObject})".update.apply
    }
  }

  override def update(imageMetaInformation: ImageMetaInformation, externalId: String): Unit = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val json = write(imageMetaInformation)
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)

    DB localTx {implicit session =>
      sql"update imagemetadata set metadata = ${dataObject} where external_id = ${externalId}".update.apply
    }
  }

  override def containsExternalId(externalId: String): Boolean = {
    DB readOnly{implicit session =>
      sql"select id from imagemetadata where external_id = ${externalId}".map(rs => rs.long("id")).single.apply.isDefined
    }
  }

  def foreach(func: ImageMetaInformation => Unit) = {
    DB readOnly { implicit session =>
      sql"select id, metadata from imagemetadata".foreach { rs =>
        func(asImageMetaInformation(rs.long("id").toString, rs.string("metadata")))
      }
    }
  }

  def asImageMetaInformation(documentId: String, json: String): ImageMetaInformation = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    val meta = read[ImageMetaInformation](json)
    ImageMetaInformation(
      documentId,
      meta.titles,
      meta.alttexts,
      ImageVariants(
        meta.images.small.flatMap(s => Option(Image(ImageApiProperties.ContextRoot + s.url, s.size, s.contentType))),
        meta.images.full.flatMap(f => Option(Image(ImageApiProperties.ContextRoot + f.url, f.size, f.contentType)))),
      meta.copyright,
      meta.tags)
  }
}
