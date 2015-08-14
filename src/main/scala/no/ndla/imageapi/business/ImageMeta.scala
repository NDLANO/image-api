package no.ndla.imageapi.business

import model.ImageMetaInformation

trait ImageMeta {

  def all(): Iterable[ImageMetaInformation]
  def withId(id: String): Option[ImageMetaInformation]
  def withTags(tags: Iterable[String]): Iterable[ImageMetaInformation]

  def containsExternalId(externalId: String): Boolean
  def upload(imageMetaInformation: ImageMetaInformation, externalId: String)

}
