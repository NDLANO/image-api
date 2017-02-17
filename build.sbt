import java.util.Properties

val Scalaversion = "2.11.8"
val Scalatraversion = "2.5.0"
val ScalaLoggingVersion = "3.1.0"
val Log4JVersion = "2.6"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.11.46"
val ScalaTestVersion = "2.2.4"
val MockitoVersion = "1.10.19"

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
    scalacOptions := Seq("-target:jvm-1.8"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.11",
      "ndla" %% "mapping" % "0.2",
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
      "org.scalikejdbc" %% "scalikejdbc" % "2.2.8",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "mysql" % "mysql-connector-java" % "5.1.36",
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "1.1.5",
      "org.scalatest" % "scalatest_2.11" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % "4.0",
      "io.searchbox" % "jest" % "2.0.0",
      "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.3.0",
      "org.elasticsearch" % "elasticsearch" % "2.3.3" % "test",
      "org.apache.lucene" % "lucene-test-framework" % "5.5.0" % "test",
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.14",
      "io.swagger" %% "swagger-scala-module" % "1.0.3",
      "io.swagger" % "swagger-servlet" % "1.5.12"
    )
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

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

// Don't run Integration tests in default run
testOptions in Test += Tests.Argument("-l", "no.ndla.imageapi.IntegrationTest")

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("java")

    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
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