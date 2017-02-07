package no.ndla.imageapi.service

import java.awt.image.BufferedImage

import no.ndla.imageapi.model.domain.ImageStream
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import javax.imageio.ImageIO

import org.imgscalr.Scalr
import scala.util.Try
import Math.{min, max, abs}


trait ImageConverter {
  val imageConverter: ImageConverter
  case class CropOptions(x1: Int, y1: Int, x2: Int, y2: Int)

  class ImageConverter {
    case class ConvertedImage(stream: InputStream, fileName: String, contentType: String) extends ImageStream

    private[service] def toConvertedImage(bufferedImage: BufferedImage, originalImage: ImageStream): ConvertedImage = {
      val outputStream = new ByteArrayOutputStream()
      ImageIO.write(bufferedImage, originalImage.format, outputStream)
      ConvertedImage(new ByteArrayInputStream(outputStream.toByteArray), originalImage.fileName, originalImage.contentType)
    }

    def resize(originalImage: ImageStream, targetWidth: Int, targetHeight: Int): Try[ImageStream] = {
      val sourceImage = ImageIO.read(originalImage.stream)
      Try(Scalr.resize(sourceImage, min(targetWidth, sourceImage.getWidth), min(targetHeight, sourceImage.getHeight)))
        .map(resized => toConvertedImage(resized, originalImage))
    }

    def resize(originalImage: ImageStream, targetSize: Int): Try[ImageStream] = {
      val sourceImage = ImageIO.read(originalImage.stream)
      val maxSize = max(sourceImage.getHeight, sourceImage.getWidth)
      Try(Scalr.resize(sourceImage, min(targetSize, maxSize))).map(resized => toConvertedImage(resized, originalImage))
    }

    def crop(originalImage: ImageStream, options: CropOptions): Try[ImageStream] = {
      val sourceImage = ImageIO.read(originalImage.stream)
      val transformedCoords = transformCoordinates(options)
      val (width, height) = getWidthHeight(transformedCoords, sourceImage)

      Try(Scalr.crop(sourceImage, transformedCoords.x1, transformedCoords.y1, width, height))
        .map(cropped => toConvertedImage(cropped, originalImage))
    }

    // Given two sets of coordinates; reorganize them so that the first coordinate is the top-left,
    // and the other coordinate is the bottom-right
    private[service] def transformCoordinates(opts: CropOptions): CropOptions = {
      val (x1, y1) = (min(opts.x1, opts.x2), min(opts.y1, opts.y2))
      val (x2, y2) = (max(opts.x1, opts.x2), max(opts.y1, opts.y2))

      CropOptions(valueOrMinimum0(x1), valueOrMinimum0(y1),
                  valueOrMinimum0(x2), valueOrMinimum0(y2))
    }

    private def valueOrMinimum0(value: Int): Int = max(value, 0)

    private[service] def getWidthHeight(opts: CropOptions, image: BufferedImage): (Int, Int) = {
      val width = abs(opts.x2 - opts.x1)
      val height = abs(opts.y2 - opts.y1)
      (min(width, image.getWidth - opts.x1), min(height, image.getHeight - opts.y1))
    }

  }
}
