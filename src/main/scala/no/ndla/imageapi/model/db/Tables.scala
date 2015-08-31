/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.model.db

import slick.driver.PostgresDriver.api._
import slick.lifted.ForeignKeyQuery

object Tables extends {
  val profile = slick.driver.PostgresDriver
} with Tables

trait Tables {

  case class Image(id: Long, url: String, size: Int, contentType:String)
  class Images(tag: Tag) extends Table[Image](tag, "image") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def url = column[String]("url")
    def size = column[Int]("size")
    def contentType = column[String]("content_type")
    def * = (id, url, size, contentType) <>(Image.tupled, Image.unapply)
  }
  lazy val images = TableQuery[Images]

  case class ImageMeta(id: Long, license: String, origin: String, smallId: Long, fullId: Long, externalId: String)
  class ImageMetas(tag: Tag) extends Table[ImageMeta](tag, "imagemeta") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def license = column[String]("license")
    def origin = column[String]("origin")
    def smallId = column[Long]("small_id")
    def fullId = column[Long]("full_id")
    def externalId = column[String]("external_id")
    def * = (id, license, origin, smallId, fullId, externalId) <>(ImageMeta.tupled, ImageMeta.unapply)

    def small: ForeignKeyQuery[Images, Image] = foreignKey("imagemeta_small_id_fkey", smallId, TableQuery[Images])(_.id)
    def full: ForeignKeyQuery[Images, Image] = foreignKey("imagemeta_full_id_fkey", fullId, TableQuery[Images])(_.id)
  }
  lazy val imagemetas = TableQuery[ImageMetas]

  case class ImageTag(id: Long, tag: String, language: Option[String], imageMetaId: Long)
  class ImageTags(dbtag: Tag) extends Table[ImageTag](dbtag, "imagetag") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def tag = column[String]("tag")
    def language = column[String]("language")
    def imageMetaId = column[Long]("imagemeta_id")
    def * = (id, tag, language.?, imageMetaId) <> (ImageTag.tupled, ImageTag.unapply)

    def imageMeta: ForeignKeyQuery[ImageMetas, ImageMeta] = foreignKey("imagetag_imagemeta_id_fkey", imageMetaId, TableQuery[ImageMetas])(_.id)
  }
  lazy val imagetags = TableQuery[ImageTags]

  case class ImageAuthor(id: Long, typeAuthor: String, name: String, imageMetaId: Long)
  class ImageAuthors(tag: Tag) extends Table[ImageAuthor](tag, "imageauthor") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def typeAuthor = column[String]("type")
    def name = column[String]("name")
    def imageMetaId = column[Long]("imagemeta_id")
    def * = (id, typeAuthor, name, imageMetaId) <>(ImageAuthor.tupled, ImageAuthor.unapply)

    def imageMeta: ForeignKeyQuery[ImageMetas, ImageMeta] = foreignKey("imageauthor_imagemeta_id_fkey", imageMetaId, TableQuery[ImageMetas])(_.id)
  }
  lazy val authors = TableQuery[ImageAuthors]

  case class ImageTitle(id: Long, title: String, language: Option[String], imageMetaId: Long)
  class ImageTitles(tag: Tag) extends Table[ImageTitle](tag, "imagetitle") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def language = column[String]("language")
    def imageMetaId = column[Long]("imagemeta_id")
    def * = (id, title, language.?, imageMetaId) <>(ImageTitle.tupled, ImageTitle.unapply)

    def imageMeta: ForeignKeyQuery[ImageMetas, ImageMeta] = foreignKey("imagetitle_imagemeta_id_fkey", imageMetaId, TableQuery[ImageMetas])(_.id)
  }
  lazy val imagetitles = TableQuery[ImageTitles]

  case class ImageLicense(id: String, description: String, url: Option[String])
  class ImageLicenses(tag: Tag) extends Table[ImageLicense](tag, "imagelicense") {
    def id = column[String]("id", O.PrimaryKey)
    def description = column[String]("description")
    def url = column[String]("url")
    def * = (id, description, url.?) <>(ImageLicense.tupled, ImageLicense.unapply)

  }
  lazy val imagelicenses = TableQuery[ImageLicenses]
}