/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.business

import no.ndla.imageapi.model.ImageMetaInformation

trait ImageMeta {

  def withId(id: String): Option[ImageMetaInformation]
  def containsExternalId(externalId: String): Boolean
  def insert(imageMetaInformation: ImageMetaInformation, externalId: String)
  def update(imageMetaInformation: ImageMetaInformation, externalId: String)

}
