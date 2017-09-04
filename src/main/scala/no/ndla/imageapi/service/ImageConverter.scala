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
  case class PixelPoint(x: Int, y: Int) // A point given with pixles
  case class PercentPoint(x: Int, y: Int) { // A point given with values from MinValue to MaxValue. MinValue,MinValue is top-left, MaxValue,MaxValue is bottom-right
    import PercentPoint._
    if (!inRange(x) || !inRange(y))
      throw new ValidationException(errors=Seq(ValidationMessage("PercentPoint", s"Invalid value for a PixelPoint. Must be in range $MinValue-$MaxValue")))

    lazy val normalizedX: Double = normalise(x)
    lazy val normalizedY: Double = normalise(y)
  }

  object PercentPoint {
    val MinValue: Int = 0
    val MaxValue: Int = 100

    private def inRange(n: Double): Boolean = n >= MinValue && n <= MaxValue
    private def normalise(coord: Int): Double = coord.toDouble / MaxValue.toDouble
  }

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

    private def crop(image: ImageStream, sourceImage: BufferedImage, topLeft: PixelPoint, bottomRight: PixelPoint): Try[ImageStream] = {
      val (width, height) = getWidthHeight(topLeft, bottomRight, sourceImage)

      Try(Scalr.crop(sourceImage, topLeft.x, topLeft.y, width, height))
        .map(cropped => toImageSteam(cropped, image))
    }

    def crop(originalImage: ImageStream, start: PercentPoint, end: PercentPoint): Try[ImageStream] = {
      val sourceImage = ImageIO.read(originalImage.stream)
      val (topLeft, bottomRight) = transformCoordinates(sourceImage, start, end)
      crop(originalImage, sourceImage, topLeft, bottomRight)
    }

    private def getStartEnd(size: Int, targetSize: Int, focal: Int): (Int, Int) = {
      val ts = min(targetSize, size) / 2
      val (start, end) = (focal - ts, focal + ts)
      val (startRem, endRem) = (abs(min(start, 0)), max(end - size, 0))

      (max(start - endRem, 0), min(end + startRem, size))
    }

    def dynamicCrop(image: ImageStream, percentFocalPoint: PercentPoint, targetWidth: Int, targetHeight: Int): Try[ImageStream] = {
      val sourceImage = ImageIO.read(image.stream)
      val focalPoint = toPixelPoint(percentFocalPoint, sourceImage)
      val (imageWidth, imageHeight) = (sourceImage.getWidth, sourceImage.getHeight)

      val (startY, endY) = getStartEnd(imageHeight, targetHeight, focalPoint.y)
      val (startX, endX) = getStartEnd(imageWidth, targetWidth, focalPoint.x)

      crop(image, sourceImage, PixelPoint(startX, startY), PixelPoint(endX, endY))
    }

    // Given two sets of coordinates; reorganize them so that the first coordinate is the top-left,
    // and the other coordinate is the bottom-right
    private[service] def transformCoordinates(image: BufferedImage, start: PercentPoint, end: PercentPoint): (PixelPoint, PixelPoint) = {
      val topLeft = PercentPoint(min(start.x, end.x), min(start.y, end.y))
      val bottomRight = PercentPoint(max(start.x, end.x), max(start.y, end.y))

      (toPixelPoint(topLeft, image), toPixelPoint(bottomRight, image))
    }

    private def toPixelPoint(point: PercentPoint, image: BufferedImage) = {
      val (width, height) = (image.getWidth, image.getHeight)
      PixelPoint((point.normalizedX * width).toInt, (point.normalizedY * height).toInt)
    }

    private[service] def getWidthHeight(start: PixelPoint, end: PixelPoint, image: BufferedImage): (Int, Int) = {
      val width = abs(start.x - end.x)
      val height = abs(start.y - end.y)
      (min(width, image.getWidth - start.x), min(height, image.getHeight - start.y))
    }

  }
}
