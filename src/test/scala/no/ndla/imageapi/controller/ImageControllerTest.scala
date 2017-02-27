/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.model.api.{Copyright, ImageMetaInformation, License, NewImageMetaInformation}
import no.ndla.imageapi.{ImageSwagger, TestEnvironment, UnitSuite}
import no.ndla.imageapi.ImageApiProperties.MaxImageFileSizeBytes
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatra.servlet.FileItem
import org.scalatra.test.Uploadable
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class ImageControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  implicit val swagger = new ImageSwagger
  lazy val controller = new ImageController
  addServlet(controller, "/*")

  case class PretendFile(content: Array[Byte], contentType: String, fileName: String) extends Uploadable {
    override def contentLength: Long = content.length
  }

  val sampleUploadFile = PretendFile(Array[Byte](-1, -40, -1), "image/jpeg", "image.jpg")

  val sampleNewImageMeta =
    """
      |{
      |    "titles": [{
      |        "title": "Utedo med hjerte på døra",
      |        "language": "nb"
      |    }],
      |    "alttexts": [{
      |        "alttext": "En skeiv utedodør med utskåret hjerte. Foto.",
      |        "language": "nb"
      |    }],
      |    "captions": [],
      |    "copyright": {
      |        "origin": "http://www.scanpix.no",
      |        "authors": [],
      |        "license": {
      |            "description": "Creative Commons Attribution-ShareAlike 2.0 Generic",
      |            "url": "https://creativecommons.org/licenses/by-sa/2.0/",
      |            "license": "by-nc-sa"
      |        }
      |    }
      |}
      |
    """.stripMargin

  test("That POST / returns 400 if parameters are missing") {
    post("/", ("metadata", sampleNewImageMeta)) {
      status should equal (400)
    }
  }

  test("That POST / returns 200 if everything went well") {
    val sampleImageMeta = ImageMetaInformation("1", "http://some.url/id", Seq.empty, Seq.empty, "http://some.url/img.jpg", 1024, "image/jpeg", Copyright(License("by", "description", None), "", Seq.empty), Seq.empty, Seq.empty)
    when(writeService.storeNewImage(any[NewImageMetaInformation], any[FileItem])).thenReturn(Success(sampleImageMeta))

    post("/", Map("metadata" -> sampleNewImageMeta), Map("file" -> sampleUploadFile)) {
      status should equal (200)
    }
  }

  test("That POST / returns 413 if file is too big") {
    val content: Array[Byte] = Array.fill(MaxImageFileSizeBytes + 1) { 0 }
    post("/", Map("metadata" -> sampleNewImageMeta), Map("file" -> sampleUploadFile.copy(content))) {
      status should equal (413)
    }
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    when(writeService.storeNewImage(any[NewImageMetaInformation], any[FileItem])).thenReturn(Failure(mock[RuntimeException]))

    post("/", Map("metadata" -> sampleNewImageMeta), Map("file" -> sampleUploadFile)) {
      status should equal (500)
    }
  }

}
