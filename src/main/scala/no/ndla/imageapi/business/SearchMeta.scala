/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.business

import no.ndla.imageapi.model.ImageMetaSummary


trait SearchMeta {
  def withTags(tags: Iterable[String], minimumSize:Option[Int], language: Option[String], license: Option[String]): Iterable[ImageMetaSummary]
  def all(minimumSize:Option[Int], license: Option[String]): Iterable[ImageMetaSummary]
}
