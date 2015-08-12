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

  val PageSize = 100

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

  override def withTags(tag: String): Iterable[ImageMetaInformation] = {
    val db = Database.forDataSource(dataSource)
    try {
      val query = Tables.imagetags.filter(_.tag === tag).flatMap(_.imageMeta)
      val result = Await.result(db.run(query.result), Duration.Inf)

      result.map(mapImage(_, db))
    } finally db.close()
  }

  def mapImage(imageMeta: Tables.ImageMeta, db:PostgresDriver.backend.Database): ImageMetaInformation = {
    val fullImage = Await.result(db.run(
      Tables.images.filter(_.id === imageMeta.fullId).result.headOption), Duration.Inf).
      map(img => model.Image(img.size, img.url))

    val smallImage = Await.result(db.run(
      Tables.images.filter(_.id === imageMeta.smallId).result.headOption), Duration.Inf).
      map(img => model.Image(img.size, img.url))

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

  override def upload(imageMetaInformation: ImageMetaInformation) = {}

  override def create() = {}

  override def exists(): Boolean = {false}
}
