import model.ImageData
import org.json4s.{Formats, DefaultFormats}
import org.scalatra.ScalatraServlet

import org.scalatra.json._

import scala.io.Source

class ImageController (protected implicit override val jsonFormats: Formats = DefaultFormats) extends ScalatraServlet with JacksonJsonSupport {

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }


  // List images
  get("/") {
    params.get("tags") match {
      case Some(tags) => ImageData.all.filter(_.tags.map(_.toLowerCase()).contains(tags.toLowerCase()))
      case None => ImageData.all
    }
  }

  get("/:image_id") {
    ImageData.all find (_.id.equals(params("image_id"))) match {
      case Some(b) => b
      case None => halt(404)
    }
  }

  get("/thumbs/:name") {
    ImageData.all find (_.thumbPath.equals("images/thumbs/" + params("name"))) match {
      case Some(image) => {
        contentType="image/jpeg"
        getClass.getResourceAsStream(image.thumbPath)
      }
      case None => halt(404)
    }
  }

  get("/full/:name") {
    ImageData.all find (_.imagePath.equals("images/full/" + params("name"))) match {
      case Some(image) => {
        contentType="image/jpeg"
        getClass.getResourceAsStream(image.imagePath)
      }
      case None => halt(404)
    }
  }
}
