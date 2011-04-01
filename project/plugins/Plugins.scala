import sbt._
class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val scctRepo = "scct-repo" at "http://mtkopone.github.com/scct/maven-repo/"
  val akkaRepo = "akka-repo" at "http://akka.io/repository"

  val scctPlugin = "reaktor" % "sbt-scct-for-2.8" % "0.1-SNAPSHOT"
  val akkaPlugin = "se.scalablesolutions.akka" % "akka-sbt-plugin" % "1.0-RC2"

  val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.4.0"
}
