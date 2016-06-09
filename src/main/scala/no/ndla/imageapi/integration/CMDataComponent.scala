package no.ndla.imageapi.integration

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
import no.ndla.imageapi.service.{ImageAuthor, ImageLicense, ImageMeta, ImageOrigin}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool, NamedDB, _}

trait CMDataComponent {
  val cmData: CMData

  class CMData(cmHost:Option[String], cmPort:Option[String], cmDatabase:Option[String], cmUser:Option[String], cmPassword:Option[String]){
    val host = cmHost.getOrElse(throw new RuntimeException("Missing host"))
    val port = cmPort.getOrElse(throw new RuntimeException("Missing host"))
    val database = cmDatabase.getOrElse(throw new RuntimeException("Missing database"))
    val user = cmUser.getOrElse(throw new RuntimeException("Missing user"))
    val password = cmPassword.getOrElse(throw new RuntimeException("Missing password"))

    Class.forName("com.mysql.jdbc.Driver")

    val cmDatasource = new MysqlConnectionPoolDataSource
    cmDatasource.setPassword(password)
    cmDatasource.setUser(user)
    cmDatasource.setUrl(s"jdbc:mysql://$host:$port/$database")

    ConnectionPool.add('cm, new DataSourceConnectionPool(cmDatasource))

    def imageMeta(imageId: String): Option[ImageMeta] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
             select n.nid, n.tnid, n.language as language, n.title,
               cfat.field_alt_text_value as alttext,
               from_unixtime(n.changed) as changed,
               REPLACE(f.filepath, 'sites/default/files/images/', '') as original,
               f.filemime as original_mime,
               f.filesize as original_size
             from node n
               left join image i on (n.nid = i.nid)
               left join files f on (f.fid = i.fid)
               left join content_field_alt_text cfat on (cfat.nid = n.nid and cfat.vid = n.vid)
             where n.type = "image" and n.status = 1 and i.image_size = "_original" and n.nid = ${imageId}
            """.stripMargin.map(rs =>
                ImageMeta(
                  rs.string("nid"),
                  rs.string("tnid"),
                  rs.string("language"),
                  rs.string("title"),
                  rs.string("alttext"),
                  rs.string("changed"),
                  rs.string("original"),
                  rs.string("original_mime"),
                  rs.string("original_size")
                )).single().apply()
      }
    }

    def imageMetaTranslations(imageId: String): List[ImageMeta] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
          select n.nid, n.tnid, n.language as language, n.title,
            cfat.field_alt_text_value as alttext,
            from_unixtime(n.changed) as changed,
            REPLACE(f.filepath, 'sites/default/files/images/', '') as original,
            f.filemime as original_mime,
            f.filesize as original_size
          from node n
            left join image i on (n.nid = i.nid)
            left join files f on (i.fid = f.fid)
            left join content_field_alt_text cfat on (cfat.nid = n.nid)
          where n.type = "image" and n.status = 1 and i.image_size = "_original"
            and n.nid = ${imageId} and n.tnid != ${imageId}
            """.stripMargin.map(rs =>
          ImageMeta(
            rs.string("nid"),
            rs.string("tnid"),
            rs.string("language"),
            rs.string("title"),
            rs.string("alttext"),
            rs.string("changed"),
            rs.string("original"),
            rs.string("original_mime"),
            rs.string("original_size")
          )).list().apply()
      }
    }

    def imageLicence(nodeId: String): Option[ImageLicense] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
              SELECT n.nid, n.tnid, cc.license FROM node n
              LEFT JOIN creativecommons_lite cc ON (n.nid = cc.nid)
              WHERE n.type = "image" AND n.nid=${nodeId}
              AND n.status = 1 AND cc.license IS NOT NULL
          """.stripMargin
          .map(rs =>
            ImageLicense(
              rs.string("nid"),
              rs.string("tnid"),
              rs.string("license")
            )).single().apply()
      }
    }

    def imageAuthor(imageId: String): List[ImageAuthor] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
          SELECT n.nid AS image_nid, n.tnid, td.name AS author_type, person.title AS author FROM node n
            LEFT JOIN ndla_authors na ON n.vid = na.vid
            LEFT JOIN term_data td ON na.tid = td.tid
            LEFT JOIN node person ON person.nid = na.person_nid
          WHERE n.type = 'image' and n.status = 1
            AND person.title is not null
            AND td.name is not null
            AND n.nid = ${imageId}
        """.stripMargin
          .map(rs =>
            ImageAuthor(
              rs.string("image_nid"),
              rs.string("tnid"),
              rs.string("author_type"),
              rs.string("author"))).list().apply()
      }
    }

    def imageOrigin(imageId: String): Option[ImageOrigin] = {
      NamedDB('cm) readOnly { implicit session =>
        sql"""
          SELECT n.nid, n.tnid, url.field_url_url AS origin FROM node n
          LEFT JOIN content_field_url url ON url.vid = n.vid
          WHERE n.type = 'image' AND n.status = 1 AND url.field_url_url IS NOT NULL AND n.nid=${imageId}
        """.stripMargin
          .map(rs =>
            ImageOrigin(
              rs.string("nid"),
              rs.string("tnid"),
              rs.string("origin"))).single().apply()
      }
    }
  }
}
