import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import model.{ImageData, Image}
import no.ndla.imageapi.business.{ImageBucket, ImageMeta}
import no.ndla.imageapi.integration.{AmazonImageMeta, AmazonIntegration}
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
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting applies on anonymous access"),
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

  val imageMeta: ImageMeta = AmazonIntegration.getImageMeta()
  val imageBucket: ImageBucket = AmazonIntegration.getImageBucket()

  // List images
  get("/", operation(getImages)) {
    params.get("tags") match {
      case Some(tags) => imageMeta.withTags(tags.toLowerCase())
      case None => imageMeta.all
    }
  }

  get("/:image_id", operation(getByImageId)) {
    imageMeta.withId(params("image_id")) match {
      case Some(image) => image
      case None => halt(404)
    }
  }

  get("/thumbs/:name") {
    imageBucket.get("thumbs/" + params("name")) match {
      case Some(image) => {
        contentType = image._1
        image._2
      }
      case None => halt(404)
    }
  }

  get("/full/:name") {
    imageBucket.get("full/" + params("name")) match {
      case Some(image) => {
        contentType = image._1
        image._2
      }
      case None => halt(404)
    }
  }
}
