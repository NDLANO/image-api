val Scalaversion = "2.11.6"
val Scalatraversion = "2.3.1"
val SwaggerUIVersion = "2.0.24"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.9.0"
val ScalaTestVersion = "2.2.4"
val MockitoVersion = "1.10.19"
val SlickVersion = "3.0.0"


lazy val commonSettings = Seq(
  organization := "ndla",
  version := "0.1-SNAPSHOT",
  scalaVersion := Scalaversion
)

lazy val image_api = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "image-api",
    libraryDependencies ++= Seq(
      "ndla" %% "logging" % "0.1-SNAPSHOT",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.json4s"   %% "json4s-native" % "3.2.11",
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "org.webjars" % "swagger-ui" % SwaggerUIVersion,
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "com.amazonaws" % "aws-java-sdk-dynamodb" % AwsSdkversion,
      "org.scalatest" % "scalatest_2.11" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test")
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

unmanagedResourceDirectories in Compile <+= (baseDirectory) {_ / "src/main/webapp"}

assemblyJarName in assembly := "image-api.jar"
mainClass in assembly := Some("no.ndla.imageapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run
testOptions in Test += Tests.Argument("-l", "no.ndla.IntegrationTest")

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("java")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

val gitHeadCommitSha = settingKey[String]("current git commit SHA")
gitHeadCommitSha in ThisBuild := Process("git log --pretty=format:%h -n 1").lines.head

imageNames in docker := Seq(
  ImageName("ndla/image-api"),
  ImageName(namespace = Some(organization.value),
    repository = name.value,
    tag = Some("v" + version.value + "_" + gitHeadCommitSha.value))
)

publishTo := {
  val nexus = "https://nexus.knowit.no/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/ndla-snapshots")
  else
    Some("releases"  at nexus + "content/repositories/ndla-releases")
}

resolvers ++= Seq(
  "Snapshot Sonatype Nexus Repository Manager" at "https://nexus.knowit.no/content/repositories/ndla-snapshots",
  "Release Sonatype Nexus Repository Manager" at "https://nexus.knowit.no/content/repositories/ndla-releases"
)

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.knowit.no", "ndla", "ndla")