import java.util.Properties

val Scalaversion = "2.12.7"
val Scalatraversion = "2.6.3"
val ScalaLoggingVersion = "3.9.0"
val ScalaTestVersion = "3.0.5"
val Log4JVersion = "2.11.1"
val Jettyversion = "9.4.12.v20180830"
val AwsSdkversion = "1.11.434"
val MockitoVersion = "2.23.0"
val Elastic4sVersion = "6.3.7"
val JacksonVersion = "2.9.8"
val ElasticsearchVersion = "6.3.2"
val Json4SVersion = "3.5.4"
val FlywayVersion = "5.2.0"
val PostgresVersion = "42.2.5"
val HikariConnectionPoolVersion = "3.2.0"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val image_api = (project in file("."))
  .settings(
    name := "image-api",
    organization := appProperties.value.getProperty("NDLAOrganization"),
    version := appProperties.value.getProperty("NDLAComponentVersion"),
    scalaVersion := Scalaversion,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.38",
      "ndla" %% "mapping" % "0.10",
      "joda-time" % "joda-time" % "2.10",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-swagger" % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SVersion,
      "org.scalikejdbc" %% "scalikejdbc" % "3.3.1",
      "org.postgresql" % "postgresql" % PostgresVersion,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "2.4.1",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-core" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % "5.2.0",
      "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-embedded" % Elastic4sVersion % "test",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion, // Overriding jackson-databind used in dependencies because of https://app.snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-72884
      "org.jsoup" % "jsoup" % "1.11.3",
      "log4j" % "log4j" % "1.2.16",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9", // This is needed for javamelody graphing
      "org.imgscalr" % "imgscalr-lib" % "4.2",
      "io.lemonlabs" %% "scala-uri" % "1.3.1",
      // These are not strictly needed, for most cases, but offers better handling of loading images with encoding issues
      "com.twelvemonkeys.imageio" % "imageio-core" % "3.4.1",
      "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.4.1",
      "commons-io" % "commons-io" % "2.6"
    )
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JettyPlugin)

val checkfmt = taskKey[Boolean]("Check for code style errors")
checkfmt := {
  val noErrorsInMainFiles = (Compile / scalafmtCheck).value
  val noErrorsInTestFiles = (Test / scalafmtCheck).value
  val noErrorsInSbtConfigFiles = (Compile / scalafmtSbtCheck).value

  noErrorsInMainFiles && noErrorsInTestFiles && noErrorsInSbtConfigFiles
}

Test / test := (Test / test).dependsOn(Test / checkfmt).value

val fmt = taskKey[Unit]("Automatically apply code style fixes")
fmt := {
  (Compile / scalafmt).value
  (Test / scalafmt).value
  (Compile / scalafmtSbt).value
}

assembly / assemblyJarName := "image-api.jar"
assembly / mainClass := Some("no.ndla.imageapi.JettyLauncher")
assembly / assemblyMergeStrategy := {
  case "mime.types"                                                  => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")          => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")        => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker := (docker dependsOn assembly).value

docker / dockerfile := {
  val artifact = (assembly / assemblyOutputPath).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("openjdk:8-jre-alpine")
    run("apk", "--no-cache", "add", "ttf-dejavu")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-XX:MaxRAMFraction=2", "-jar", artifactTargetPath)
  }
}

docker / imageNames := Seq(
  ImageName(namespace = Some(organization.value),
            repository = name.value,
            tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

Test / parallelExecution := false

resolvers ++= scala.util.Properties
  .envOrNone("NDLA_RELEASES")
  .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
  .toSeq
