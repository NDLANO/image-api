package model

case class Image(id:String, thumbPath:String, imagePath:String, tags:List[String])

object ImageData {

  val all = List(
    Image("1", "images/thumbs/Bjørn.jpg", "images/full/Bjørn.jpg", List("bjørn", "rovdyr")),
    Image("2", "images/thumbs/Elg.jpg", "images/full/Elg.jpg", List("elg")),
    Image("3", "images/thumbs/Jerv.jpg", "images/full/Jerv.jpg", List("jerv", "rovdyr")),
    Image("4", "images/thumbs/Mink.jpg", "images/full/Mink.jpg", List("mink")),
    Image("5", "images/thumbs/Rein.jpg", "images/full/Rein.jpg", List("rein"))
  )

}
