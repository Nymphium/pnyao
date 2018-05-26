name := "Pyano"
organization := "com.github.nymphium"
version := "1.0"
scalaVersion := "2.12.5"
scalacOptions ++= Seq("-unchecked", "-deprecation")
libraryDependencies ++= Seq(
		"com.itextpdf" % "itextpdf" % "5.5.7",
		"commons-io" % "commons-io" % "2.5",
		"org.bouncycastle" % "bcpkix-jdk15on" % "1.50"
		)

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
		"io.circe" %% "circe-core",
		"io.circe" %% "circe-generic",
		"io.circe" %% "circe-parser"
		).map(_ % circeVersion)

