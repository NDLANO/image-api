import java.util.Properties

val Scalaversion = "2.12.2"
val Scalatraversion = "2.5.1"
val ScalaLoggingVersion = "3.5.0"
val Log4JVersion = "2.9.1"
val Jettyversion = "9.4.9.v20180320"
val AwsSdkversion = "1.11.46"
val ScalaTestVersion = "3.0.1"
val MockitoVersion = "1.10.19"
val Elastic4sVersion = "6.1.4"
val JacksonVersion = "2.9.5"
val ElasticsearchVersion = "6.0.1"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val commonSettings = Seq(
  organization := appProperties.value.getProperty("NDLAOrganization"),
  version := appProperties.value.getProperty("NDLAComponentVersion"),
  scalaVersion := Scalaversion
)

lazy val image_api = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "image-api",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.29",
      "ndla" %% "mapping" % "0.7",
      "joda-time" % "joda-time" % "2.8.2",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.json4s"   %% "json4s-native" % "3.5.0",
      "org.scalikejdbc" %% "scalikejdbc" % "2.5.0",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % "4.0",
      "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "6.0.1",
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion, // Overriding jackson-databind used in elastic4s because of https://snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-32111
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "org.apache.lucene" % "lucene-test-framework" % "6.4.1" % "test",
      "org.jsoup" % "jsoup" % "1.11.2",
      "org.imgscalr" % "imgscalr-lib" % "4.2",
      "com.netaporter" %% "scala-uri" % "0.4.16",
      // These are not strictly needed, for most cases, but offers better handling of loading images with encoding issues
      "com.twelvemonkeys.imageio" % "imageio-core" % "3.3.2",
      "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.3.2",
      "commons-io" % "commons-io" % "2.6"
    )
  ).enablePlugins(DockerPlugin)
   .enablePlugins(GitVersioning)
   .enablePlugins(JettyPlugin)

assemblyJarName in assembly := "image-api.jar"
mainClass in assembly := Some("no.ndla.imageapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")  => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")  => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class")  => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run on Travis as there is no elasticsearch localhost:9200 there yet.
// NB this line will unfortunalty override runs on your local commandline so that
// sbt "test-only -- -n no.ndla.tag.IntegrationTest"
// will not run unless this line gets commented out or you remove the tag over the test class
// This should be solved better!
testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker := (docker dependsOn assembly).value

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("openjdk:8-jre-alpine")

    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-Xmx512M", "-jar", artifactTargetPath)
  }
}

val gitHeadCommitSha = settingKey[String]("current git commit SHA")
gitHeadCommitSha in ThisBuild := Process("git log --pretty=format:%h -n 1").lines.head

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

parallelExecution in Test := false

resolvers ++= scala.util.Properties.envOrNone("NDLA_RELEASES").map(repo => "Release Sonatype Nexus Repository Manager" at repo).toSeq
