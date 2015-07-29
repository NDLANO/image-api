package model

case class Image(id:Long, title:String, thumbPath:String, imagePath:String, tags:List[String])

object ImageData {

  val all = List(
    Image(1, "Bjørn i busk", "images/thumbs/Bjørn.jpg", "images/full/Bjørn.jpg", List("bjørn", "rovdyr")),
    Image(2, "Elg i busk", "images/thumbs/Elg.jpg", "images/full/Elg.jpg", List("elg")),
    Image(3, "Liten jerv", "images/thumbs/Jerv.jpg", "images/full/Jerv.jpg", List("jerv", "rovdyr")),
    Image(4, "Overrasket mink", "images/thumbs/Mink.jpg", "images/full/Mink.jpg", List("mink")),
    Image(5, "Rein har fanget rødtopp", "images/thumbs/Rein.jpg", "images/full/Rein.jpg", List("rein"))
  )

  val alle = List(
    Image(1, "Bjørn i busk", "thumbs/Bjørn.jpg", "full/Bjørn.jpg", List("bjørn", "rovdyr")),
    Image(2, "Elg i busk", "thumbs/Elg.jpg", "full/Elg.jpg", List("elg")),
    Image(3, "Liten jerv", "thumbs/Jerv.jpg", "full/Jerv.jpg", List("jerv", "rovdyr")),
    Image(4, "Overrasket mink", "thumbs/Mink.jpg", "full/Mink.jpg", List("mink")),
    Image(5, "Rein har fanget rødtopp", "thumbs/Rein.jpg", "full/Rein.jpg", List("rein"))
  )
}
