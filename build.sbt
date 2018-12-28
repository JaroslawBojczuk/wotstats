name := """wotstats"""
organization := "com.vasth"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test

libraryDependencies += "org.json4s" %% "json4s-native" % "3.3.0"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.3.0"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.0"

libraryDependencies += "com.lihaoyi" %% "requests" % "0.1.4"
libraryDependencies += "com.lihaoyi" %% "upickle" % "0.7.1"

libraryDependencies += "com.typesafe.slick" %% "slick" % "3.2.3"
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0"
libraryDependencies += "org.mariadb.jdbc" % "mariadb-java-client" % "1.5.2"

libraryDependencies += "joda-time" % "joda-time" % "2.10.1"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"