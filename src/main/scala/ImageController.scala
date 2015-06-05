import org.scalatra.ScalatraServlet

class ImageController extends ScalatraServlet {

  // List images
  get("/") {
    params.get("name") match {
        // TODO: Filtrer på tagnavn? Eller egen søketag?
        case Some(name) => "Hello " + name
        case None => "Hello world"
    }
  }

  get("/:image_id") {
    val name = params.getOrElse("image_id", "world")
    "Hello (with image_id) " + name
  }
}
