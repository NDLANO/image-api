import model.{ImageData, Image}
import org.json4s.{Formats, DefaultFormats}
import org.scalatra.ScalatraServlet

import org.scalatra.json._
import org.scalatra.swagger.{SwaggerSupport, Swagger}

class ImageController (implicit val swagger:Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  // Swagger-stuff
  protected val applicationDescription = "API for accessing images from ndla.no. It exposes operations for browsing and searching lists of images, and retrieving single images."

  val getImages =
    (apiOperation[List[Image]]("getImages")
      summary "Show all images"
      notes "Shows all the images in the ndla.no database. You can search it too."
      parameters (
        queryParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access"),
        queryParam[Option[String]]("tags").description("Return only images with submitted tag. Multiple tags may be entered comma separated, and will give results matching either one of them."),
        queryParam[Option[String]]("minimumSize").description("Return only images with full size larger than submitted value in KiloBytes")
      ))

  val getByImageId =
    (apiOperation[Image]("findByImageId")
      summary "Show image info"
      notes "Shows info of the image with submitted id"
      parameter pathParam[String]("image_id").description("Image_id of the image that needs to be fetched"))
  // End of Swagger-stuff

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  // List images
  get("/", operation(getImages)) {
    params.get("tags") match {
      case Some(tags) => ImageData.all.filter(_.tags.map(_.toLowerCase()).contains(tags.toLowerCase()))
      case None => ImageData.all
    }
  }

  get("/:image_id", operation(getByImageId)) {
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
