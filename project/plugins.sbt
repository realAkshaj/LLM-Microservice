addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13"