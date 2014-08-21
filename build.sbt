name := "play-jaas"

organization := "net.duicu"

version := "1.1.1"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "org.opensaml" % "opensaml" % "2.6.1",
  "org.opensaml" % "openws" % "1.5.1",
  "org.opensaml" % "xmltooling" % "1.4.1"
)

publishArtifact in Test := false

publishMavenStyle := true

// disable using the Scala version in output paths and artifacts
//crossPaths := false

lazy val playJaas = (project in file(".")).enablePlugins(PlayJava)
