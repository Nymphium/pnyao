name := "pnyao"
version := "1.0"

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

mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
    // Keep the content for all reference-overrides.conf files
    MergeStrategy.concat
  case PathList(ps @ _*) if ps.last endsWith "Log.class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith "LogConfigurationException.class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith "LogFactory.class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith "SimpleLog$1.class" => MergeStrategy.first
  case x =>
    // For all the other files, use the default sbt-assembly merge strategy
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

