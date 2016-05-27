/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.batch

import java.io._
import java.net.URL

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model._
import no.ndla.imageapi.integration.AmazonIntegration

import scala.io.Source


object ImageApiUploader extends LazyLogging {

  def main(args: Array[String]) {

    //    logger.info("Antall argumenter: " + args.length)
    //    logger.info("Argumenter: " + args)
    //
    //    if (args.length != 2) {
    //      logger.info("Two arguments required: <path to input files> <range to run>")
    //      System.exit(1)
    //    }
    //
    //    val rangeFrom = args(0).split("-")(0).toInt
    //    val rangeTo = args(0).split("-")(1).toInt


    val rangeFrom = 0
    val rangeTo = 10
    BatchComponentRegistry.importService.doImport(rangeFrom, rangeTo)
  }
}

case class ImageMeta(nid:String, tnid:String, language:String, title:String, alttext:String, changed:String, originalFile:String, originalMime: String, originalSize: String)
case class ImageLicense(nid:String, license:String)
case class ImageAuthor(nid: String, typeAuthor:String, name:String)
case class ImageOrigin(nid: String, origin:String)