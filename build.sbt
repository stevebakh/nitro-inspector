name := "nitro-splitter"

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-feature")

libraryDependencies ++=  Seq(
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4")
