package no.ndla.imageapi.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.lang.Math.{abs, max, min}
import javax.imageio.ImageIO

import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import no.ndla.imageapi.model.domain.ImageStream
import org.imgscalr.Scalr
import org.imgscalr.Scalr.Mode

import scala.util.Try


trait ImageConverter {
  val imageConverter: ImageConverter
  case class PercentPoint(x: Double, y: Double) { // A point given with values from 0 to 1. 0,0 is top-left, 1,1 is bottom-right
    private def inRange(n: Double): Boolean = n >= 0 && n <= 1

    if (!inRange(x) || !inRange(y))
      throw new ValidationException(errors=Seq(ValidationMessage("PercentPoint", s"Invalid value for a PixelPoint. Must be in range 0-1")))
  }
  case class PixelPoint(x: Int, y: Int) // A point given with pixles

  class ImageConverter {
    private[service] def toImageSteam(bufferedImage: BufferedImage, originalImage: ImageStream): ImageStream = {
      val outputStream = new ByteArrayOutputStream()
      ImageIO.write(bufferedImage, originalImage.format, outputStream)
      new ImageStream {
        override def stream = new ByteArrayInputStream(outputStream.toByteArray)
        override def contentType: String = originalImage.contentType
        override def fileName: String = originalImage.fileName
      }
    }

    def resize(originalImage: ImageStream, targetWidth: Int, targetHeight: Int): Try[ImageStream] = {
      val sourceImage = ImageIO.read(originalImage.stream)
      Try(Scalr.resize(sourceImage, min(targetWidth, sourceImage.getWidth), min(targetHeight, sourceImage.getHeight)))
        .map(resized => toImageSteam(resized, originalImage))
    }

    def resize(originalImage: ImageStream, mode: Mode, targetSize: Int): Try[ImageStream] = {
      val MaxTargetSize = 2000
      val sourceImage = ImageIO.read(originalImage.stream)
      Try(Scalr.resize(sourceImage, mode, min(targetSize, MaxTargetSize))).map(resized => toImageSteam(resized, originalImage))
    }

    def resizeWidth(originalImage: ImageStream, size: Int): Try[ImageStream] = resize(originalImage, Mode.FIT_TO_WIDTH, size)

    def resizeHeight(originalImage: ImageStream, size: Int): Try[ImageStream] = resize(originalImage, Mode.FIT_TO_HEIGHT, size)

    def crop(originalImage: ImageStream, start: PercentPoint, end: PercentPoint): Try[ImageStream] = {
      val sourceImage = ImageIO.read(originalImage.stream)
      val (topLeft, bottomRight) = transformCoordinates(sourceImage, start, end)
      val (width, height) = getWidthHeight(topLeft, bottomRight, sourceImage)

      Try(Scalr.crop(sourceImage, topLeft.x, topLeft.y, width, height))
        .map(cropped => toImageSteam(cropped, originalImage))
    }

    // Given two sets of coordinates; reorganize them so that the first coordinate is the top-left,
    // and the other coordinate is the bottom-right
    private[service] def transformCoordinates(image: BufferedImage, start: PercentPoint, end: PercentPoint): (PixelPoint, PixelPoint) = {
      val topLeft = PercentPoint(min(start.x, end.x), min(start.y, end.y))
      val bottomRight = PercentPoint(max(start.x, end.x), max(start.y, end.y))

      val (width, height) = (image.getWidth, image.getHeight)

      (PixelPoint((topLeft.x * width).toInt, (topLeft.y * height).toInt), PixelPoint((bottomRight.x * width).toInt, (bottomRight.y * height).toInt))
    }

    private[service] def getWidthHeight(start: PixelPoint, end: PixelPoint, image: BufferedImage): (Int, Int) = {
      val width = abs(start.x - end.x)
      val height = abs(start.y - end.y)
      (min(width, image.getWidth - start.x), min(height, image.getHeight - start.y))
    }

  }
}
