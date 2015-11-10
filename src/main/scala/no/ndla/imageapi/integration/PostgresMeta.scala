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
import no.ndla.imageapi.network.ApplicationUrl
import org.postgresql.util.PGobject
import scalikejdbc._


class PostgresMeta(dataSource: DataSource) extends ImageMeta with LazyLogging {

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override def withId(id: String): Option[ImageMetaInformation] = {
    DB readOnly {implicit session =>
      sql"select metadata from imagemetadata where id = ${id.toInt}".map(rs => rs.string("metadata")).single.apply match {
        case Some(json) => Option(asImageMetaInformationWithApplicationUrl(id, json))
        case None => None
      }
    }
  }

  override def withExternalId(externalId: String): Option[ImageMetaInformation] = {
    DB readOnly {implicit session =>
      sql"select id, metadata from imagemetadata where external_id = ${externalId}".map(rs => (rs.long("id"), rs.string("metadata"))).single.apply match {
        case Some((id, meta)) => Option(asImageMetaInformationWithRelUrl(id.toString, meta))
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

  def elements: List[ImageMetaInformation] = {
    DB readOnly { implicit session =>
      sql"select id, metadata from imagemetadata".map(rs => {
        asImageMetaInformationWithRelUrl(rs.long("id").toString, rs.string("metadata"))
      }).list.apply()
    }
  }

  def foreach(func: ImageMetaInformation => Unit) = {
    elements.foreach { el =>
      func(el)
    }
    /*
    DB readOnly { implicit session =>
      sql"select id, metadata from imagemetadata".foreach { rs =>
        func(asImageMetaInformationWithRelUrl(rs.long("id").toString, rs.string("metadata")))
      }
    }
    */
  }

  def asImageMetaInformationWithApplicationUrl(documentId: String, json: String): ImageMetaInformation = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    val meta = read[ImageMetaInformation](json)
    ImageMetaInformation(
      documentId,
      meta.titles,
      meta.alttexts,
      ImageVariants(
        meta.images.small.flatMap(s => Option(Image(ApplicationUrl.get + s.url, s.size, s.contentType))),
        meta.images.full.flatMap(f => Option(Image(ApplicationUrl.get + f.url, f.size, f.contentType)))),
      meta.copyright,
      meta.tags)
  }

  def asImageMetaInformationWithRelUrl(documentId: String, json: String): ImageMetaInformation = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    val meta = read[ImageMetaInformation](json)
    ImageMetaInformation(
      documentId,
      meta.titles,
      meta.alttexts,
      meta.images,
      meta.copyright,
      meta.tags)
  }
}
