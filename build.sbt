import Dependencies.*
//import com.typesafe.sbt.SbtNativePackager.autoImport.maintainer

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.textgeneration"

// Explicitly set Java version
ThisBuild / javacOptions ++= Seq("-source", "11", "-target", "11")

lazy val root = (project in file("."))
  .settings(
    name := "bedrock-grpc-client",
    libraryDependencies ++= Seq(
      akkaHttp,
      akkaStream,
      akkaActor,
      sprayJson,
      logback,
      scalaTest,
      ollama,
      http,
      json4s,
      cors,
      "io.grpc" % "grpc-netty" % "1.54.0",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % "10.5.0" % Test,
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"

    ),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )