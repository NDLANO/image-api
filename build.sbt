val Scalaversion = "2.11.6"
val Scalatraversion = "2.3.1"
val Jettyversion = "9.2.10.v20150310"

lazy val commonSettings = Seq(
  organization := "no.ndla",
  version := "0.1",
  scalaVersion := Scalaversion
)

lazy val image_api = (project in file(".")).
  settings(jetty(): _*).
  settings(commonSettings: _*).
  settings(
  name := "image-api",
  libraryDependencies ++= Seq(
    "org.scalatra" %% "scalatra" % Scalatraversion,
    "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container",
    "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
    "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test")
)
