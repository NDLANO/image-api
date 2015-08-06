package model

case class ImageMetaInformation(id:String, title:String, images:ImageVariants, copyright:Copyright, tags:List[String])
case class ImageVariants(small: Image, full: Image)
case class Image(url:String, size:String)
case class Copyright(license:String, origin:String, authors:List[Author])
case class Author(`type`:String, name:String)
