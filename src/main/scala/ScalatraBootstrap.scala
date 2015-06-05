import javax.servlet.ServletContext

import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    // Mount servlets.
    context.mount(new ImageController, "/images/*")
  }

}
