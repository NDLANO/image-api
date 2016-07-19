/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.imageapi.model.domain

case class ImageMetaInformation(id: String, titles: List[ImageTitle], alttexts: List[ImageAltText], images: ImageVariants, copyright: Copyright, tags: List[ImageTag])
case class ImageTitle(title: String, language: Option[String])
case class ImageAltText(alttext: String, language: Option[String])
case class ImageTag(tags: Seq[String], language: Option[String])
case class ImageVariants(small: Option[Image], full: Option[Image])
case class Image(url: String, size: Int, contentType: String)
case class Copyright(license: License, origin: String, authors: List[Author])
case class License(license: String, description: String, url: Option[String])
case class Author(`type`: String, name: String)
