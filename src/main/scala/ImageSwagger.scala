import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{NativeSwaggerBase, ApiInfo, Swagger}

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

object ImagesApiInfo extends ApiInfo (
  "Images Api",
  "Documentation for the Images API of NDLA.no",
  "http://ndla.no",
  "kontakt-epost",
  "GPL2.0",
  "lisensurl")

class ImageSwagger extends Swagger(Swagger.SpecVersion, "1.0.0", ImagesApiInfo)
