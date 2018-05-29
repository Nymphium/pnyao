name := "pyano"

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "com.itextpdf" % "itextpdf" % "5.5.7",
  "commons-io" % "commons-io" % "2.5",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.50",
  "com.lihaoyi" %% "scalatags" % "0.6.7"
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
