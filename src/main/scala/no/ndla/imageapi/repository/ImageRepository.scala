/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.integration.DataSource
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.service.ConverterService
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._


trait ImageRepository {
  this: DataSource with ConverterService =>
  val imageRepository: ImageRepository

  class ImageRepository extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats + ImageMetaInformation.JSonSerializer

    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def withId(id: Long): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.id = $id")
      }
    }

    def withExternalId(externalId: String): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.external_id = $externalId")
      }
    }

    def insert(imageMeta: ImageMetaInformation, externalId: Option[String] = None)(implicit session: DBSession = AutoSession) = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(imageMeta))

      val imageId = externalId match {
        case Some(ext) => sql"insert into imagemetadata(external_id, metadata) values (${ext}, ${dataObject})".updateAndReturnGeneratedKey.apply
        case None => sql"insert into imagemetadata(metadata) values (${dataObject})".updateAndReturnGeneratedKey.apply
      }

      imageMeta.copy(id = Some(imageId))
    }

    def insertWithExternalId(imageMetaInformation: ImageMetaInformation, externalId: String): ImageMetaInformation = {
      val json = write(imageMetaInformation)

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        val imageId = sql"insert into imagemetadata(external_id, metadata) values(${externalId}, ${dataObject})".updateAndReturnGeneratedKey.apply
        imageMetaInformation.copy(id = Some(imageId))
      }
    }

    def update(imageMetaInformation: ImageMetaInformation, id: Long): ImageMetaInformation = {
      val json = write(imageMetaInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        sql"update imagemetadata set metadata = ${dataObject} where id = ${id}".update.apply
        imageMetaInformation.copy(id = Some(id))
      }
    }

    def delete(imageId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from imagemetadata where id = ${imageId}".update.apply
    }

    def minMaxId: (Long, Long) = {
      DB readOnly { implicit session =>
        sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from imagemetadata".map(rs => {
          (rs.long("mi"), rs.long("ma"))
        }).single().apply() match {
          case Some(minmax) => minmax
          case None => (0L, 0L)
        }
      }
    }

    def imagesWithIdBetween(min: Long, max: Long): List[ImageMetaInformation] = imageMetaInformationsWhere(sqls"im.id between $min and $max")

    private def imageMetaInformationWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"select ${im.result.*} from ${ImageMetaInformation.as(im)} where $whereClause".map(ImageMetaInformation(im)).single().apply()
    }

    private def imageMetaInformationsWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): List[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"select ${im.result.*} from ${ImageMetaInformation.as(im)} where $whereClause".map(ImageMetaInformation(im)).list.apply()
    }
  }
}