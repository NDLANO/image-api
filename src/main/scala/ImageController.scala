import model.{ImageMetaInformation}
import no.ndla.imageapi.business.{ImageStorage, ImageMeta}
import no.ndla.imageapi.integration.AmazonIntegration
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json._
import org.scalatra.swagger.{Swagger, SwaggerSupport}

class ImageController (implicit val swagger:Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  // Swagger-stuff
  protected val applicationDescription = "API for accessing images from ndla.no."

  val getImages =
    (apiOperation[List[ImageMetaInformation]]("getImages")
      summary "Show all images"
      notes "Shows all the images in the ndla.no database. You can search it too."
      parameters (
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access"),
        queryParam[Option[String]]("tags").description("Return only images with submitted tag. Multiple tags may be entered comma separated, and will give results matching either one of them."),
        queryParam[Option[String]]("minimumSize").description("Return only images with full size larger than submitted value in KiloBytes")
      ))

  val getByImageId =
    (apiOperation[ImageMetaInformation]("findByImageId")
      summary "Show image info"
      notes "Shows info of the image with submitted id"
      parameter pathParam[String]("image_id").description("Image_id of the image that needs to be fetched"))


  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  val imageMeta: ImageMeta = AmazonIntegration.getImageMeta()
  val imageStorage: ImageStorage = AmazonIntegration.getImageStorage()


  get("/", operation(getImages)) {
    params.get("tags") match {
      case Some(tags) => imageMeta.withTags(tags.toLowerCase())
      case None => imageMeta.all
    }
  }

  get("/:image_id", operation(getByImageId)) {
    imageMeta.withId(params("image_id")) match {
      case Some(image) => image
      case None => None
    }
  }

  get("/thumbs/:name") {
    imageStorage.get("thumbs/" + params("name")) match {
      case Some(image) => {
        contentType = image._1
        image._2
      }
      case None => None
    }
  }

  get("/full/:name") {
    imageStorage.get("full/" + params("name")) match {
      case Some(image) => {
        contentType = image._1
        image._2
      }
      case None => None
    }
  }
}
