name := "pnyao"
version := "1.0-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")
scalaVersion := "2.12.5"

val circeVersion = "0.9.3"
libraryDependencies ++= Seq(
	guice,
	"org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2",
	"com.lihaoyi" %% "scalatags" % "0.6.7"
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

lazy val pnyaoInternal = project in file("pnyao")
lazy val root = (project in file("."))
	.enablePlugins(PlayScala)
	.aggregate(pnyaoInternal)
	.dependsOn(pnyaoInternal)

