package no.ndla.imageapi.business

import model.ImageMetaInformation

trait ImageMeta {

  def all(minimumSize:Option[Int]): Iterable[ImageMetaInformation]
  def withId(id: String): Option[ImageMetaInformation]
  def withTags(tags: Iterable[String], minimumSize:Option[Int]): Iterable[ImageMetaInformation]

  def containsExternalId(externalId: String): Boolean
  def upload(imageMetaInformation: ImageMetaInformation, externalId: String)

}
