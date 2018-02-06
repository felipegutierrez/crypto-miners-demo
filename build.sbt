import play.sbt.PlayImport._

name := """crypto-miners-demo"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += guice
libraryDependencies += evolutions
libraryDependencies += jdbc
libraryDependencies += filters
libraryDependencies += ws

libraryDependencies += "com.h2database" % "h2" % "1.4.194"
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.5.3"
libraryDependencies += specs2 % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test

libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0"
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.19.3"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.2.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.2.0"
libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.7.2"

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5"
