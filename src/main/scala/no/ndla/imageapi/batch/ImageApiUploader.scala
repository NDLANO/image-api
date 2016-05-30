/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.batch

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ComponentRegistry


object ImageApiUploader extends LazyLogging {

  def main(args: Array[String]) {
    ComponentRegistry.importService.doImport(0, 10)
  }
}

case class ImageMeta(nid:String, tnid:String, language:String, title:String, alttext:String, changed:String, originalFile:String, originalMime: String, originalSize: String)
case class ImageLicense(nid:String, license:String)
case class ImageAuthor(nid: String, typeAuthor:String, name:String)
case class ImageOrigin(nid: String, origin:String)
