val Scalatraversion = "2.3.0"
val scalatra = "org.scalatra" %% "scalatra" % Scalatraversion
val scalate = "org.scalatra" %% "scalatra-scalate" % Scalatraversion

lazy val commonSettings = Seq(
  organization := "no.ndla",
  version := "0.1",
  scalaVersion := "2.11.4"
)

lazy val image_api = (project in file(".")).
  settings(commonSettings: _*).
  settings(
  name := "image-api",
  libraryDependencies += scalatra
)
