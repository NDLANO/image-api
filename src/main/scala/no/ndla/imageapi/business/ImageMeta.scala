package no.ndla.imageapi.business

import model.ImageMetaInformation

trait ImageMeta {

  def all(): Iterable[ImageMetaInformation]
  def withId(id: String): Option[ImageMetaInformation]
  def withTags(tag: String): Iterable[ImageMetaInformation]

  def upload(imageMetaInformation: ImageMetaInformation)

  def exists(): Boolean
  def create()

}
