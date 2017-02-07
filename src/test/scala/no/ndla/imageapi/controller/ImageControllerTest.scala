package no.ndla.imageapi.controller

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import no.ndla.imageapi.TestData.NdlaLogoImage
import no.ndla.imageapi.{ImageSwagger, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraSuite

import scala.util.Success

class ImageControllerTest extends UnitSuite with ScalatraSuite with TestEnvironment {
  implicit val swagger = new ImageSwagger
  val imageName = "ndla_logo.jpg"

  override val imageConverter = new ImageConverter
  lazy val controller = new ImageController
  addServlet(controller, "/*")

  override def beforeEach = {
    when(imageStorage.get(any[String])).thenReturn(Success(NdlaLogoImage))
  }

  test("That GET /full/image.jpg returns 200 if image was found") {
    get(s"/full/$imageName") {
      status should equal (200)

      val image = ImageIO.read(new ByteArrayInputStream(bodyBytes))
      image.getWidth should equal(189)
      image.getHeight should equal(60)
    }
  }

  test("That GET /full/image.jpg with resizing returns a resized image") {
    get(s"/full/$imageName?width=100") {
      status should equal (200)

      val image = ImageIO.read(new ByteArrayInputStream(bodyBytes))
      image.getWidth should equal(100)
    }
  }

  test("That GET /full/image.jpg with cropping returns a cropped image") {
    get(s"/full/$imageName?cropStart=0,0&cropEnd=20,20") {
      status should equal (200)

      val image = ImageIO.read(new ByteArrayInputStream(bodyBytes))
      image.getWidth should equal(20)
      image.getHeight should equal(20)
    }
  }

  test("That GET /full/image.jpg with cropping and resizing returns a cropped and resized image") {
    get(s"/full/$imageName?cropStart=0,0&cropEnd=100,20&width=50") {
      status should equal (200)

      val image = ImageIO.read(new ByteArrayInputStream(bodyBytes))
      image.getWidth should equal(50)
      image.getHeight should equal(10)
    }
  }
}
