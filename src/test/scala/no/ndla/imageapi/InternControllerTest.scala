package no.ndla.imageapi

import no.ndla.imageapi.model._
import org.json4s.jackson.Serialization._
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.{Failure, Success}

class InternControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {

  lazy val controller = new InternController
  addServlet(controller, "/*")

  val DefaultImageMetaInformation = ImageMetaInformation("1", "http://api.test.ndla.no/images/1", List(), List(), ImageVariants(None, None), Copyright(License("", "", None), "", List()), List())

  test("That GET /extern/abc returns 404") {
    get("/extern/abc") {
      status should equal (404)
    }
  }

  test("That GET /extern/123 returns 404 if 123 is not found") {
    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(None)
    get("/extern/123") {
      status should equal (404)
    }
  }

  test("That GET /extern/123 returns 200 and imagemeta when found") {
    implicit val formats = org.json4s.DefaultFormats

    when(imageRepository.withExternalId(eqTo("123"))).thenReturn(Some(DefaultImageMetaInformation))
    get("/extern/123") {
      status should equal (200)
      body should equal (write(DefaultImageMetaInformation))
    }
  }

  test("That POST /import/123 returns 200 OK when import is a success") {
    when(importService.importImage(eqTo("123"))).thenReturn(Success(DefaultImageMetaInformation))
    post("/import/123") {
      status should equal (200)
    }
  }

  test("That POST /import/123 returns 500 with error message when import failed") {
    when(importService.importImage(eqTo("123"))).thenReturn(Failure(new NullPointerException("null")))
    post("/import/123") {
      status should equal (500)
      body indexOf "external_id 123 failed after" should be > 0
    }
  }

}
