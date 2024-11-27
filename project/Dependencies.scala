import sbt.*

object Dependencies {
  lazy val akkaVersion = "2.8.0"
  lazy val akkaHttpVersion = "10.5.0"
  lazy val ollamaVersion = "1.0.79"
  lazy val httpVersion = "4.5.13"
  lazy val json4sVersion = "4.0.6"
  lazy val corsVersion = "1.2.0"

  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  lazy val sprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion

  // Using an older version of logback that's compatible with Java 11
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15" % Test

  lazy val ollama = "io.github.ollama4j" % "ollama4j" % ollamaVersion
  lazy val http = "org.apache.httpcomponents" % "httpclient" % httpVersion
  lazy val json4s = "org.json4s" %% "json4s-native" % json4sVersion
  lazy val cors = "ch.megard" %% "akka-http-cors" % corsVersion
}