package no.ndla.imageapi.service

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.lang.Math.{abs, max, min}
import javax.imageio.ImageIO

import no.ndla.imageapi.model.domain.ImageStream
import org.imgscalr.Scalr
import org.imgscalr.Scalr.Mode

import scala.util.Try


trait ImageConverter {
  val imageConverter: ImageConverter
  case class Point(x: Int, y: Int)

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
      val sourceImage = ImageIO.read(originalImage.stream)
      val maxSize = mode match {
        case Mode.FIT_TO_WIDTH => sourceImage.getWidth
        case Mode.FIT_TO_HEIGHT => sourceImage.getHeight
        case _ => max(sourceImage.getWidth, sourceImage.getHeight)
      }
      Try(Scalr.resize(sourceImage, mode, min(targetSize, maxSize))).map(resized => toImageSteam(resized, originalImage))
    }

    def resizeWidth(originalImage: ImageStream, size: Int): Try[ImageStream] = resize(originalImage, Mode.FIT_TO_WIDTH, size)

    def resizeHeight(originalImage: ImageStream, size: Int): Try[ImageStream] = resize(originalImage, Mode.FIT_TO_HEIGHT, size)

    def crop(originalImage: ImageStream, start: Point, end: Point): Try[ImageStream] = {
      val sourceImage = ImageIO.read(originalImage.stream)
      val (topLeft, bottomRight) = transformCoordinates(start, end)
      val (width, height) = getWidthHeight(topLeft, bottomRight, sourceImage)

      Try(Scalr.crop(sourceImage, topLeft.x, topLeft.y, width, height))
        .map(cropped => toImageSteam(cropped, originalImage))
    }

    // Given two sets of coordinates; reorganize them so that the first coordinate is the top-left,
    // and the other coordinate is the bottom-right
    private[service] def transformCoordinates(start: Point, end: Point): (Point, Point) = {
      val topLeft = Point(min(start.x, end.x), min(start.y, end.y))
      val bottomRight = Point(max(start.x, end.x), max(start.y, end.y))

      (Point(max(topLeft.x, 0), max(topLeft.y, 0)), Point(max(bottomRight.x, 0), max(bottomRight.y, 0)))
    }

    private[service] def getWidthHeight(start: Point, end: Point, image: BufferedImage): (Int, Int) = {
      val width = abs(start.x - end.x)
      val height = abs(start.y - end.y)
      (min(width, image.getWidth - start.x), min(height, image.getHeight - start.y))
    }

  }
}
