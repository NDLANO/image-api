/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import java.util.Date

import no.ndla.imageapi.model.{ImageNotFoundException, Language, api, domain}
import no.ndla.imageapi.model.api.{NewImageMetaInformationV2, SearchResult, UpdateImageMetaInformation}
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.{ImageSwagger, TestData, TestEnvironment, UnitSuite}
import no.ndla.imageapi.ImageApiProperties.MaxImageFileSizeBytes
import org.json4s.DefaultFormats
import org.json4s.native.JsonParser
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatra.servlet.FileItem
import org.scalatra.test.Uploadable
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success, Try}

class ImageControllerV2Test extends UnitSuite with ScalatraSuite with TestEnvironment {

  val jwtHeader = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"

  val jwtClaims = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiaW1hZ2VzOndyaXRlIl0sIm5kbGFfaWQiOiJhYmMxMjMifSwibmFtZSI6IkRvbmFsZCBEdWNrIiwiaXNzIjoiaHR0cHM6Ly9zb21lLWRvbWFpbi8iLCJzdWIiOiJnb29nbGUtb2F1dGgyfDEyMyIsImF1ZCI6ImFiYyIsImV4cCI6MTQ4NjA3MDA2MywiaWF0IjoxNDg2MDM0MDYzfQ"
  val jwtClaimsNoRoles = "eyJhcHBfbWV0YWRhdGEiOnsibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9"
  val jwtClaimsWrongRole = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXJ0aWNsZXM6d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9"

  val authHeaderWithWriteRole = s"Bearer $jwtHeader.$jwtClaims._Hva8XNvTDOtU2nLSnYvqVgtLtAcvGipHFsBXZxJknw"
  val authHeaderWithoutAnyRoles = s"Bearer $jwtHeader.$jwtClaimsNoRoles.eNEK5datycKuV292kOxT4IhCMvrrq0KpSyH8C69mdnM"
  val authHeaderWithWrongRole = s"Bearer $jwtHeader.$jwtClaimsWrongRole.VxqM2bu2UF8IAalibIgdRdmsTDDWKEYpKzHPbCJcFzA"

  implicit val swagger = new ImageSwagger
  override val converterService = new ConverterService
  lazy val controller = new ImageControllerV2
  addServlet(controller, "/*")

  case class PretendFile(content: Array[Byte], contentType: String, fileName: String) extends Uploadable {
    override def contentLength: Long = content.length
  }

  val sampleUploadFile = PretendFile(Array[Byte](-1, -40, -1), "image/jpeg", "image.jpg")

  val sampleNewImageMetaV2 =
    """
      |{
      |  "title":"test1",
      |  "alttext":"test2",
      |  "copyright": {
      |    "license": {
      |    "license": "by-sa",
      |    "description": "Creative Commons Attribution-ShareAlike 2.0 Generic",
      |    "url": "https:\/\/creativecommons.org\/licenses\/by-sa\/2.0\/"
      |  },
      |    "origin": "",
      |    "authors": [
      |  {
      |    "type": "Forfatter",
      |    "name": "Wenche Heir"
      |  }
      |    ]
      |  },
      |  "tags": [
      |    "lel"
      |  ],
      |  "caption": "captionheredude",
      |  "language": "no"
      |}
    """.stripMargin

  val sampleUpdateImageMeta =
    """
      |{
      | "title":"TestTittel",
      | "alttext":"TestAltText",
      | "language":"nb"
      |}
    """.stripMargin

  test("That POST / returns 400 if parameters are missing") {
    post("/", Map("metadata" -> sampleNewImageMetaV2), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("That POST / returns 200 if everything went well") {
    val titles: Seq[ImageTitle] = Seq()
    val alttexts: Seq[ImageAltText] = Seq()
    val copyright = Copyright(License("by", "description", None), "", Seq.empty, Seq.empty, Seq.empty, None, None, None)
    val tags: Seq[ImageTag] = Seq()
    val captions: Seq[ImageCaption] = Seq()

    val sampleImageMeta = ImageMetaInformation(Some(1), titles, alttexts, "http://some.url/img.jpg", 1024, "image/jpeg", copyright, tags, captions, "updatedBy", new Date())

    when(writeService.storeNewImage(any[NewImageMetaInformationV2], any[FileItem])).thenReturn(Success(sampleImageMeta))

    post("/", Map("metadata" -> sampleNewImageMetaV2), Map("file" -> sampleUploadFile), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    post("/", Map("metadata" -> sampleNewImageMetaV2)) {
      status should equal(403)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/", Map("metadata" -> sampleNewImageMetaV2), headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal(403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/", Map("metadata" -> sampleNewImageMetaV2), headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal(403)
    }
  }

  test("That POST / returns 413 if file is too big") {
    val content: Array[Byte] = Array.fill(MaxImageFileSizeBytes + 1) {
      0
    }
    post("/", Map("metadata" -> sampleNewImageMetaV2), Map("file" -> sampleUploadFile.copy(content)), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(413)
    }
  }

  test("That POST / returns 500 if an unexpected error occurs") {
    when(writeService.storeNewImage(any[NewImageMetaInformationV2], any[FileItem])).thenReturn(Failure(mock[RuntimeException]))

    post("/", Map("metadata" -> sampleNewImageMetaV2), Map("file" -> sampleUploadFile), headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(500)
    }
  }

  test("That GET / returns body and 200") {
    val expectedBody = """{"totalCount":0,"page":1,"pageSize":10,"results":[]}"""
    when(searchService.all(Option(any[Int]), Option(any[String]), Option(any[String]), Option(any[Int]), Option(any[Int]))).thenReturn(SearchResult(0, 1, 10, List()))
    get("/") {
      status should equal(200)
      body should equal(expectedBody)
    }
  }

  test("That GET / returns body and 200 when image exists") {
    val imageSummary = api.ImageMetaSummary("4", api.ImageTitle("Tittel", "nb"), api.ImageAltText("AltText", "nb"), "http://image-api.ndla-local/image-api/raw/4", "http://image-api.ndla-local/image-api/v2/images/4", "by-sa")
    val expectedBody = """{"totalCount":1,"page":1,"pageSize":10,"results":[{"id":"4","title":{"title":"Tittel","language":"nb"},"altText":{"alttext":"AltText","language":"nb"},"previewUrl":"http://image-api.ndla-local/image-api/raw/4","metaUrl":"http://image-api.ndla-local/image-api/v2/images/4","license":"by-sa"}]}"""
    when(searchService.all(Option(any[Int]), Option(any[String]), Option(any[String]), Option(any[Int]), Option(any[Int]))).thenReturn(SearchResult(1, 1, 10, List(imageSummary)))
    get("/") {
      status should equal(200)
      body should equal(expectedBody)
    }
  }

  test("That GET /<id> returns 404 when image does not exist") {
    when(imageRepository.withId(123)).thenReturn(None)
    get("/123") {
      status should equal(404)
    }
  }

  test("That GET /<id> returns body and 200 when image exists") {
    implicit val formats = DefaultFormats
    val testUrl = "http://test.test/1"
    val expectedBody = s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"by-nc-sa","description":"Creative Commons Attribution-NonCommercial-ShareAlike 2.0 Generic","url":"https://creativecommons.org/licenses/by-nc-sa/2.0/"},"origin":"http://www.scanpix.no","creators":[{"type":"Fotograf","name":"Test Testesen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[{"type":"Leverandør","name":"Leverans Leveransensen"}]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expectedObject = JsonParser.parse(expectedBody).extract[api.ImageMetaInformationV2]
    when(imageRepository.withId(1)).thenReturn(Option(TestData.elg))

    get("/1") {
      status should equal(200)
      val result = JsonParser.parse(body).extract[api.ImageMetaInformationV2]
      result.copy(imageUrl = testUrl, metaUrl = testUrl) should equal(expectedObject)
    }
  }

  test("That GET /<id> returns body with agreement license and authors") {
    when(draftApiClient.getAgreementCopyright(1)).thenReturn(Some(api.Copyright(api.License("gnu", "gnuggert", Some("https://gnuli/")), "http://www.scanpix.no", List(api.Author("Forfatter", "Knutulf Knagsen")), List(), List(), None, None, None)))
    implicit val formats = DefaultFormats
    val testUrl = "http://test.test/1"
    val expectedBody = s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreement": 1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[{"type":"Leverandør","name":"Leverans Leveransensen"}]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expectedObject = JsonParser.parse(expectedBody).extract[api.ImageMetaInformationV2]
    val agreementElg = ImageMetaInformation(Some(1), List(ImageTitle("Elg i busk", "nb")), List(ImageAltText("Elg i busk", "nb")),
      "Elg.jpg", 2865539, "image/jpeg",
      Copyright(TestData.ByNcSa, "http://www.scanpix.no", List(Author("Fotograf", "Test Testesen")), List(Author("Redaksjonelt", "Kåre Knegg")), List(Author("Leverandør", "Leverans Leveransensen")), Some(1), None, None),
      List(ImageTag(List("rovdyr", "elg"), "nb")), List(ImageCaption("Elg i busk", "nb")), "ndla124", TestData.updated())

    when(imageRepository.withId(1)).thenReturn(Option(agreementElg))

    get("/1") {
      status should equal(200)
      val result = JsonParser.parse(body).extract[api.ImageMetaInformationV2]
      result.copy(imageUrl = testUrl, metaUrl = testUrl) should equal(expectedObject)
    }
  }

  test("That PATCH /<id> returns 200 when everything went well") {
    when(writeService.updateImage(any[Long], any[UpdateImageMetaInformation])).thenReturn(Try(TestData.apiElg))
    patch("/1", sampleUpdateImageMeta, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("That PATCH /<id> returns 404 when image doesn't exist") {
    when(writeService.updateImage(any[Long], any[UpdateImageMetaInformation])).thenThrow(new ImageNotFoundException(s"Image with id 1 found"))
    patch("/1", sampleUpdateImageMeta, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(404)
    }
  }

  test("That PATCH /<id> returns 403 when not permitted") {
    patch("/1", Map("metadata" -> sampleUpdateImageMeta), headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal(403)
    }
  }

}
