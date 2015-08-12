package model.db

import slick.driver.PostgresDriver.api._
import slick.lifted.ForeignKeyQuery

object Tables extends {
  val profile = slick.driver.PostgresDriver
} with Tables

trait Tables {

  case class Image(id: Long, size: String, url: String)
  class Images(tag: Tag) extends Table[Image](tag, "image") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def size = column[String]("size")
    def url = column[String]("url")
    def * = (id, size, url) <>(Image.tupled, Image.unapply)
  }
  lazy val images = TableQuery[Images]

  case class ImageMeta(id: Long, title: String, license: String, origin: String, smallId: Long, fullId: Long, externalId: String)
  class ImageMetas(tag: Tag) extends Table[ImageMeta](tag, "imagemeta") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def license = column[String]("license")
    def origin = column[String]("origin")
    def smallId = column[Long]("small_id")
    def fullId = column[Long]("full_id")
    def externalId = column[String]("external_id")
    def * = (id, title, license, origin, smallId, fullId, externalId) <>(ImageMeta.tupled, ImageMeta.unapply)

    def small: ForeignKeyQuery[Images, Image] = foreignKey("imagemeta_small_id_fkey", smallId, TableQuery[Images])(_.id)
    def full: ForeignKeyQuery[Images, Image] = foreignKey("imagemeta_full_id_fkey", fullId, TableQuery[Images])(_.id)
  }
  lazy val imagemetas = TableQuery[ImageMetas]

  case class ImageTag(id: Long, tag: String, imageMetaId: Long)
  class ImageTags(dbtag: Tag) extends Table[ImageTag](dbtag, "imagetag") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def tag = column[String]("tag")
    def imageMetaId = column[Long]("imagemeta_id")
    def * = (id, tag, imageMetaId) <>(ImageTag.tupled, ImageTag.unapply)

    def imageMeta: ForeignKeyQuery[ImageMetas, ImageMeta] = foreignKey("imagetag_imagemeta_id_fkey", imageMetaId, TableQuery[ImageMetas])(_.id)
  }
  lazy val imagetags = TableQuery[ImageTags]

  case class ImageAuthor(id: Long, typeAuthor: String, name: String, imageMetaId: Long)
  class ImageAuthors(tag: Tag) extends Table[ImageAuthor](tag, "imageauthor") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def typeAuthor = column[String]("type")
    def name = column[String]("name")
    def imageMetaId = column[Long]("imagemeta_id")
    def * = (id, name, typeAuthor, imageMetaId) <>(ImageAuthor.tupled, ImageAuthor.unapply)

    def imageMeta: ForeignKeyQuery[ImageMetas, ImageMeta] = foreignKey("imageauthor_imagemeta_id_fkey", imageMetaId, TableQuery[ImageMetas])(_.id)
  }
  lazy val authors = TableQuery[ImageAuthors]
}