package no.ndla.imageapi.integration

import javax.sql.DataSource

import model.{Copyright, ImageVariants, ImageMetaInformation}
import model.db._
import no.ndla.imageapi.business.ImageMeta
import slick.driver.PostgresDriver

import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DbImageMeta(dataSource: DataSource) extends ImageMeta {

  val PageSize = 10

  override def all(): Iterable[ImageMetaInformation] = {
    val db = Database.forDataSource(dataSource)
    try {
      val query = Tables.imagemetas.take(PageSize)
      val result = Await.result(db.run(query.result), Duration.Inf)

      result.map(mapImage(_, db))
    } finally db.close()
  }

  override def withId(id: String): Option[ImageMetaInformation] = {
    val db = Database.forDataSource(dataSource)
    try {
      val query = Tables.imagemetas.filter(_.id === id.toLong)
      val result = Await.result(db.run(query.result.headOption), Duration.Inf)

      result.map(mapImage(_, db))
    } finally db.close()
  }

  override def withTags(tagList: Iterable[String]): Iterable[ImageMetaInformation] = {
    val db = Database.forDataSource(dataSource)
    try {
      val query = Tables.imagetags.filter(_.tag inSet tagList).flatMap(_.imageMeta).take(PageSize)
      val result = Await.result(db.run(query.result), Duration.Inf)

      result.map(mapImage(_, db))
    } finally db.close()
  }

  override def containsExternalId(externalId: String): Boolean = {
    val db = Database.forDataSource(dataSource)
    try {
      Await.result(db.run(
        Tables.imagemetas.filter(_.externalId === externalId).result.headOption), Duration.Inf).isDefined
    } finally db.close()
  }

  def mapImage(imageMeta: Tables.ImageMeta, db:PostgresDriver.backend.Database): ImageMetaInformation = {
    val fullImage = Await.result(db.run(
      Tables.images.filter(_.id === imageMeta.fullId).result.headOption), Duration.Inf).
      map(img => model.Image(img.url, img.size,img.contentType))

    val smallImage = Await.result(db.run(
      Tables.images.filter(_.id === imageMeta.smallId).result.headOption), Duration.Inf).
      map(img => model.Image(img.url, img.size,img.contentType))

    val imageAuthors = Await.result(db.run(
      Tables.authors.filter(_.imageMetaId === imageMeta.id).result), Duration.Inf).
      map(author => model.Author(author.typeAuthor, author.name))

    val tags = Await.result(db.run(
      Tables.imagetags.filter(_.imageMetaId === imageMeta.id).result), Duration.Inf).
      map(_.tag)

    ImageMetaInformation(imageMeta.id.toString, imageMeta.title,
      ImageVariants(smallImage, fullImage),
      Copyright(imageMeta.license, imageMeta.origin, imageAuthors),
      tags
    )
  }

  override def upload(imageMetaInformation: ImageMetaInformation, externalId: String) = {
    val db = Database.forDataSource(dataSource)
    try {

      val fullImage = imageMetaInformation.images.full.get
      val fullInsert = (Tables.images returning Tables.images.map(_.id)) += Tables.Image(0, fullImage.url, fullImage.size, fullImage.contentType)
      val fullId = Await.result(db.run(fullInsert), Duration.Inf)

      val thumbImage = imageMetaInformation.images.small.get
      val thumbInsert = (Tables.images returning Tables.images.map(_.id)) += Tables.Image(0, thumbImage.url, thumbImage.size, thumbImage.contentType)
      val thumbId = Await.result(db.run(thumbInsert), Duration.Inf)

      val insertImageMeta = Tables.ImageMeta(0, imageMetaInformation.title, imageMetaInformation.copyright.license, imageMetaInformation.copyright.origin, thumbId, fullId, externalId)
      val imageMetaId = Await.result(db.run((Tables.imagemetas returning Tables.imagemetas.map(_.id)) += insertImageMeta), Duration.Inf)

      imageMetaInformation.tags.foreach(tag => {
        Await.result(db.run(Tables.imagetags += Tables.ImageTag(0, tag, imageMetaId)), Duration.Inf)
      })

      imageMetaInformation.copyright.authors.foreach(author => {
        Await.result(db.run(Tables.authors += Tables.ImageAuthor(0, author.`type`, author.name, imageMetaId)), Duration.Inf)
      })


    } finally db.close()
  }
}
