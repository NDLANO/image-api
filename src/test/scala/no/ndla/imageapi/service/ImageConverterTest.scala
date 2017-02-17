package no.ndla.imageapi.service

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import no.ndla.imageapi.TestData.NdlaLogoImage
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class ImageConverterTest extends UnitSuite with TestEnvironment {
  val service = new ImageConverter
  val (imageWidth, imageHeight) = (1000, 1000)
  val image: BufferedImage = mock[BufferedImage]

  override def beforeEach = {
    when(image.getWidth).thenReturn(imageWidth)
    when(image.getHeight).thenReturn(imageHeight)
  }

  test("transformCoordinates returns a CoordOptions object with correctly transformed coordinates") {
    service.transformCoordinates(Point(100, 50), Point(10, 200)) should equal (Point(10, 50), Point(100, 200))
    service.transformCoordinates(Point(100, 200), Point(10, 50)) should equal (Point(10, 50), Point(100, 200))
    service.transformCoordinates(Point(10, 50), Point(100, 200)) should equal (Point(10, 50), Point(100, 200))
    service.transformCoordinates(Point(10, 200), Point(100, 50)) should equal (Point(10, 50), Point(100, 200))
  }

  test("transformCoordinates does not return coordinates < 0") {
    service.transformCoordinates(Point(-10, 200), Point(100, 50)) should equal (Point(0, 50), Point(100, 200))
    service.transformCoordinates(Point(-10, 200), Point(100, -50)) should equal (Point(0, 0), Point(100, 200))
  }

  test("getWidthHeight returns the width and height of a segment to crop") {
    service.getWidthHeight(Point(10, 200), Point(100, 50), image) should equal ((90, 150))
  }

  test("getWidthHeight returns max values if one coordinate is outside of image") {
    service.getWidthHeight(Point(10, 200), Point(imageWidth + 1, imageHeight + 1), image) should equal ((990, 800))
  }

  test("toConvertedImage converts a BufferedImage to an ImageStream") {
    val bufferedImage = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB)
    val origImage = mock[ImageStream]

    when(origImage.fileName).thenReturn("full/image.jpg")
    when(origImage.contentType).thenReturn("image/jpg")
    when(origImage.format).thenReturn("jpg")

    val result = service.toImageSteam(bufferedImage, origImage)
    result.fileName should equal (origImage.fileName)
    result.contentType should equal (origImage.contentType)
    result.format should equal (origImage.format)
  }

  test("crop crops an image according to given settings") {
    val croppedImage = service.crop(NdlaLogoImage, Point(0, 0), Point(100, 50))
    croppedImage.isSuccess should equal(true)

    val image = ImageIO.read(croppedImage.get.stream)
    image.getWidth should equal(100)
    image.getHeight should equal(50)
  }

  test("resize resizes image height correctly") {
    val resizedImage = service.resizeHeight(NdlaLogoImage, 30)
    resizedImage.isSuccess should equal(true)

    val image = ImageIO.read(resizedImage.get.stream)
    image.getHeight should equal(30)
  }

  test("resize resizes image width correctly") {
    val resizedImage = service.resizeWidth(NdlaLogoImage, 30)
    resizedImage.isSuccess should equal(true)

    val image = ImageIO.read(resizedImage.get.stream)
    image.getWidth should equal(30)
  }

  test("resize resizes an image according to image orientation if both height and width is specified") {
    val croppedImage = service.resize(NdlaLogoImage, 100, 60)
    croppedImage.isSuccess should equal(true)

    val image = ImageIO.read(croppedImage.get.stream)
    image.getWidth should equal(100)
    image.getHeight should not equal 60
  }
}
