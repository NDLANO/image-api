/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.DataSourceComponent
import no.ndla.imageapi.model.{api, domain}
import no.ndla.imageapi.service.ConverterService
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc._


trait ImageRepositoryComponent {
  this: DataSourceComponent with ConverterService =>
  val imageRepository: ImageRepository

  class ImageRepository extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats

    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def withId(id: String): Option[api.ImageMetaInformation] = {
      DB readOnly { implicit session =>
        sql"select metadata from imagemetadata where id = ${id.toInt}".map(rs => rs.string("metadata")).single.apply match {
          case Some(json) => Some(converterService.asApiImageMetaInformationWithApplicationUrl(id, read[domain.ImageMetaInformation](json)))
          case None => None
        }
      }
    }

    def withExternalId(externalId: String): Option[domain.ImageMetaInformation] = {
      DB readOnly { implicit session =>
        sql"select id, metadata from imagemetadata where external_id = ${externalId}".map(rs => (rs.long("id"), rs.string("metadata"))).single.apply match {
          case Some((id, json)) => Some(read[domain.ImageMetaInformation](json).copy(id = id.toString))
          case None => None
        }
      }
    }

    def insert(imageMetaInformation: domain.ImageMetaInformation, externalId: String): domain.ImageMetaInformation = {
      val json = write(imageMetaInformation)

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        val imageId = sql"insert into imagemetadata(external_id, metadata) values(${externalId}, ${dataObject})".updateAndReturnGeneratedKey.apply
        imageMetaInformation.copy(id = imageId.toString)
      }
    }

    def update(imageMetaInformation: domain.ImageMetaInformation, id: String): domain.ImageMetaInformation = {
      val json = write(imageMetaInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        sql"update imagemetadata set metadata = ${dataObject} where id = ${id.toLong}".update.apply
        imageMetaInformation.copy(id = id)
      }
    }

    def numElements: Int = {
      DB readOnly { implicit session =>
        sql"select count(*) from imagemetadata".map(rs => {
          rs.int("count")
        }).list.first().apply() match {
          case Some(count) => count
          case None => 0
        }
      }
    }

    def applyToAll(func: List[domain.ImageMetaInformation] => Unit) = {
      val numberOfBulks = math.ceil(numElements.toFloat / ImageApiProperties.IndexBulkSize).toInt

      DB readOnly { implicit session =>
        for(i <- 0 until numberOfBulks) {
          func(
            sql"select id,metadata from imagemetadata limit ${ImageApiProperties.IndexBulkSize} offset ${i * ImageApiProperties.IndexBulkSize}".map(rs => {
              read[domain.ImageMetaInformation](rs.string("metadata")).copy(id = rs.long("id").toString)
            }).toList.apply
          )
        }
      }
    }
  }
}