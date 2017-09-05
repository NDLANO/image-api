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
    service.transformCoordinates(image, PercentPoint(10, 5), PercentPoint(1, 20)) should equal (PixelPoint(10, 50), PixelPoint(100, 200))
    service.transformCoordinates(image, PercentPoint(10, 20),PercentPoint(1, 5)) should equal (PixelPoint(10, 50), PixelPoint(100, 200))
    service.transformCoordinates(image, PercentPoint(1, 5), PercentPoint(10, 20)) should equal (PixelPoint(10, 50), PixelPoint(100, 200))
    service.transformCoordinates(image, PercentPoint(1, 20), PercentPoint(10, 5)) should equal (PixelPoint(10, 50), PixelPoint(100, 200))
  }

  test("getWidthHeight returns the width and height of a segment to crop") {
    service.getWidthHeight(PixelPoint(10, 200), PixelPoint(100, 50), image) should equal ((90, 150))
  }

  test("getWidthHeight returns max values if one coordinate is outside of image") {
    service.getWidthHeight(PixelPoint(10, 200), PixelPoint(imageWidth + 1, imageHeight + 1), image) should equal ((990, 800))
  }

  test("toConvertedImage converts a BufferedImage to an ImageStream") {
    val bufferedImage = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB)
    val origImage = mock[ImageStream]

    when(origImage.fileName).thenReturn("full/image.jpg")
    when(origImage.contentType).thenReturn("image/jpg")
    when(origImage.format).thenReturn("jpg")

    val result = service.toImageStream(bufferedImage, origImage)
    result.fileName should equal (origImage.fileName)
    result.contentType should equal (origImage.contentType)
    result.format should equal (origImage.format)
  }

  test("crop crops an image according to given settings") {
    val croppedImage = service.crop(NdlaLogoImage, PercentPoint(0, 0), PercentPoint(50, 50))
    croppedImage.isSuccess should equal(true)

    val image = ImageIO.read(croppedImage.get.stream)
    image.getWidth should equal(94)
    image.getHeight should equal(30)
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

  test("dynamic cropping should work as expected") {
    val croppedImage = service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), Some(10), Some(30))
    val image = ImageIO.read(croppedImage.get.stream)
    image.getWidth should equal(10)
    image.getHeight should equal(30)
  }

  test("dynamic cropping should scalale according to original image size if only one dimension size is specified") {
    val image = ImageIO.read(service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), Some(100), None).get.stream)
    image.getWidth should equal(100)
    image.getHeight should equal(31)

    val image2 = ImageIO.read(service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), None, Some(50)).get.stream)
    image2.getWidth should equal(157)
    image2.getHeight should equal(50)
  }

  test("dynamoc crop should not manipulate image if neither target width or target height is specified") {
    val image = ImageIO.read(service.dynamicCrop(NdlaLogoImage, PercentPoint(0, 0), None, None).get.stream)
    image.getWidth should equal(NdlaLogoImage.sourceImage.getWidth)
    image.getHeight should equal(NdlaLogoImage.sourceImage.getHeight)
  }

}
