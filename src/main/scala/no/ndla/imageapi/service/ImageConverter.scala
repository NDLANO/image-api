package no.ndla.imageapi.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.lang.Math.{abs, max, min}
import javax.imageio.ImageIO

import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import no.ndla.imageapi.model.domain.ImageStream
import org.imgscalr.Scalr
import org.imgscalr.Scalr.Mode

import scala.util.{Success, Try}


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
    private[service] def toImageStream(bufferedImage: BufferedImage, originalImage: ImageStream): ImageStream = {
      val outputStream = new ByteArrayOutputStream()
      ImageIO.write(bufferedImage, originalImage.format, outputStream)
      new ImageStream {
        override def stream = new ByteArrayInputStream(outputStream.toByteArray)
        override def contentType: String = originalImage.contentType
        override def fileName: String = originalImage.fileName
        override lazy val sourceImage = ImageIO.read(stream)
      }
    }

    def resize(originalImage: ImageStream, targetWidth: Int, targetHeight: Int): Try[ImageStream] = {
      val sourceImage = originalImage.sourceImage
      Try(Scalr.resize(sourceImage, min(targetWidth, sourceImage.getWidth), min(targetHeight, sourceImage.getHeight)))
        .map(resized => toImageStream(resized, originalImage))
    }

    def resize(originalImage: ImageStream, mode: Mode, targetSize: Int): Try[ImageStream] = {
      val MaxTargetSize = 2000
      val sourceImage = originalImage.sourceImage
      Try(Scalr.resize(sourceImage, mode, min(targetSize, MaxTargetSize))).map(resized => toImageStream(resized, originalImage))
    }

    def resizeWidth(originalImage: ImageStream, size: Int): Try[ImageStream] = resize(originalImage, Mode.FIT_TO_WIDTH, size)

    def resizeHeight(originalImage: ImageStream, size: Int): Try[ImageStream] = resize(originalImage, Mode.FIT_TO_HEIGHT, size)

    private def crop(image: ImageStream, sourceImage: BufferedImage, topLeft: PixelPoint, bottomRight: PixelPoint): Try[ImageStream] = {
      val (width, height) = getWidthHeight(topLeft, bottomRight, sourceImage)

      Try(Scalr.crop(sourceImage, topLeft.x, topLeft.y, width, height))
        .map(cropped => toImageStream(cropped, image))
    }

    def crop(originalImage: ImageStream, start: PercentPoint, end: PercentPoint): Try[ImageStream] = {
      val sourceImage = originalImage.sourceImage
      val (topLeft, bottomRight) = transformCoordinates(sourceImage, start, end)
      crop(originalImage, sourceImage, topLeft, bottomRight)
    }

    private def getStartEndCoords(focalPoint: Int, targetDimensionSize: Int, originalDimensionSize: Int): (Int, Int) = {
      val ts = min(targetDimensionSize.toDouble, originalDimensionSize.toDouble) / 2.0
      val (start, end) = (focalPoint - ts.floor.toInt, focalPoint + ts.round.toInt)
      val (startRem, endRem) = (abs(min(start, 0)), max(end - originalDimensionSize, 0))

      (max(start - endRem, 0), min(end + startRem, originalDimensionSize))
    }

    def dynamicCrop(image: ImageStream, percentFocalPoint: PercentPoint, targetWidthOpt: Option[Int], targetHeightOpt: Option[Int]): Try[ImageStream] = {
      val sourceImage = image.sourceImage
      val focalPoint = toPixelPoint(percentFocalPoint, sourceImage)
      val (imageWidth, imageHeight) = (sourceImage.getWidth, sourceImage.getHeight)

      val (targetWidth: Int, targetHeight: Int) = (targetWidthOpt, targetHeightOpt) match {
        case (None, None) => return Success(image)
        case (Some(w), Some(h)) => (min(w, imageWidth), min(h, imageHeight))
        case (Some(w), None) =>
          val actualTargetWidth = min(imageWidth, w)
          val widthReductionPercent: Double = actualTargetWidth.toDouble / imageWidth.toDouble
          (w, (imageHeight * widthReductionPercent).toInt)
        case (None, Some(h)) =>
          val actualTargetHeight = min(imageHeight, h)
          val heightReductionPercent: Double = actualTargetHeight.toDouble / imageHeight.toDouble
          ((imageWidth * heightReductionPercent).toInt, actualTargetHeight)
      }

      val (startY, endY) = getStartEndCoords(focalPoint.y, targetHeight, imageHeight)
      val (startX, endX) = getStartEndCoords(focalPoint.x, targetWidth, imageWidth)

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
