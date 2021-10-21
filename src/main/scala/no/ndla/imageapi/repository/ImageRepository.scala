/*
 * Part of NDLA image-api.
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
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

trait ImageRepository {
  this: DataSource with ConverterService =>
  val imageRepository: ImageRepository

  class ImageRepository extends LazyLogging with Repository[ImageMetaInformation] {
    implicit val formats: Formats = ImageMetaInformation.repositorySerializer

    def imageCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
      sql"select count(*) from ${ImageMetaInformation.table}"
        .map(rs => rs.long("count"))
        .single()
        .getOrElse(0)

    def withId(id: Long): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.id = $id")
      }
    }

    def getRandomImage()(implicit session: DBSession = ReadOnlyAutoSession): Option[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"select ${im.result.*} from ${ImageMetaInformation.as(im)} where metadata is not null order by random() limit 1"
        .map(ImageMetaInformation.fromResultSet(im))
        .single()
    }

    def withExternalId(externalId: String): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.external_id = $externalId")
      }
    }

    def insert(imageMeta: ImageMetaInformation)(implicit session: DBSession = AutoSession) = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(imageMeta))

      val imageId =
        sql"insert into imagemetadata(metadata) values (${dataObject})".updateAndReturnGeneratedKey()
      imageMeta.copy(id = Some(imageId))
    }

    def insertWithExternalId(imageMetaInformation: ImageMetaInformation, externalId: String): ImageMetaInformation = {
      val json = write(imageMetaInformation)

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        val imageId =
          sql"insert into imagemetadata(external_id, metadata) values(${externalId}, ${dataObject})"
            .updateAndReturnGeneratedKey()
        imageMetaInformation.copy(id = Some(imageId))
      }
    }

    def update(imageMetaInformation: ImageMetaInformation, id: Long): ImageMetaInformation = {
      val json = write(imageMetaInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        sql"update imagemetadata set metadata = ${dataObject} where id = ${id}".update()
        imageMetaInformation.copy(id = Some(id))
      }
    }

    def delete(imageId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from imagemetadata where id = ${imageId}".update()
    }

    def minMaxId: (Long, Long) = {
      DB readOnly { implicit session =>
        sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from imagemetadata"
          .map(rs => {
            (rs.long("mi"), rs.long("ma"))
          })
          .single() match {
          case Some(minmax) => minmax
          case None         => (0L, 0L)
        }
      }
    }

    def documentsWithIdBetween(min: Long, max: Long): List[ImageMetaInformation] =
      imageMetaInformationsWhere(sqls"im.id between $min and $max")

    private def imageMetaInformationWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"select ${im.result.*} from ${ImageMetaInformation.as(im)} where $whereClause"
        .map(ImageMetaInformation.fromResultSet(im))
        .single()
    }

    private def imageMetaInformationsWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): List[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"select ${im.result.*} from ${ImageMetaInformation.as(im)} where $whereClause"
        .map(ImageMetaInformation.fromResultSet(im))
        .list()
    }

    private def escapeSQLWildcards(str: String): String = str.replace("%", "\\%")

    def getImageFromFilePath(filePath: String)(implicit session: DBSession = ReadOnlyAutoSession) = {
      val wildcardMatch = s"%${escapeSQLWildcards(filePath.dropWhile(_ == '/'))}"
      val im = ImageMetaInformation.syntax("im")
      sql"""
            select ${im.result.*}
            from ${ImageMetaInformation.as(im)}
            where metadata->>'imageUrl' like $wildcardMatch
            limit 1;
        """
        .map(ImageMetaInformation.fromResultSet(im))
        .single()
    }

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${ImageMetaInformation.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    def getByPage(pageSize: Int, offset: Int)(
        implicit session: DBSession = ReadOnlyAutoSession): Seq[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"""
           select ${im.result.*}
           from ${ImageMetaInformation.as(im)}
           where metadata is not null
           offset $offset
           limit $pageSize
      """
        .map(ImageMetaInformation.fromResultSet(im))
        .list()
    }

  }
}
