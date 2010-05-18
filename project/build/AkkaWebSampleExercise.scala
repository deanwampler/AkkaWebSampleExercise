import sbt._

// Portions adapted from http://github.com/mgutz/sbt-console-template

class AkkaWebSampleExercise(info: ProjectInfo) extends DefaultWebProject(info)  {

	override def repositories = Set(
    "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots",
		"jBoss" at "http://repository.jboss.org/maven2",
		"Multiverse Releases" at "http://multiverse.googlecode.com/svn/maven-repository/releases/",
		"GuiceyFruit" at "http://guiceyfruit.googlecode.com/svn/repo/releases/",
		"DataBinder" at "http://databinder.net/repo",
		"Configgy" at "http://www.lag.net/repo",
		"Akka Maven Repository" at "http://scalablesolutions.se/akka/repository",
		"Java.Net" at "http://download.java.net/maven/2",
		"Scala Tools" at "http://scala-tools.org/repo-releases",
    "Ibiblio" at "http://www.ibiblio.org/maven2/",
    "google" at "http://undercover.googlecode.com/svn/maven/repository")

	override def libraryDependencies = Set(

    "org.scalatest" % "scalatest" % "1.0.1-for-scala-2.8.0.Beta1-with-test-interfaces-0.3-SNAPSHOT" % "test",

		/* Embedded Jetty web server */
		"org.eclipse.jetty"  % "jetty-server"   % "7.0.2.v20100331" % "test",
		"org.eclipse.jetty"  % "jetty-webapp"   % "7.0.2.v20100331" % "test",

		/* akka dependencies */
    "se.scalablesolutions.akka" % "akka-kernel_2.8.0.Beta1"      % "0.8.1" % "compile",
    "se.scalablesolutions.akka" % "akka-core_2.8.0.Beta1"        % "0.8.1" % "compile",
    "se.scalablesolutions.akka" % "akka-camel_2.8.0.Beta1"       % "0.8.1" % "compile",
    "se.scalablesolutions.akka" % "akka-servlet_2.8.0.Beta1"     % "0.8.1" % "compile",
    "se.scalablesolutions.akka" % "akka-rest_2.8.0.Beta1"        % "0.8.1" % "compile",

    "net.liftweb" % "lift-json" % "2.0-M5" % "compile",
    "sjson.json" % "sjson" % "0.5-SNAPSHOT-2.8.Beta1" % "compile",

    "com.osinka" % "mongo-scala-driver" % "0.7.4-2.8.0.Beta1-RC6" % "compile")

  // Work around a problem where sbt incorrectly updated the Scala jars to the RC1 release.
  // (Still an issue?)
  override def jettyRunClasspath = 
    super.jettyRunClasspath --- ("target" ** "scala-library*.jar") --- ("lib_managed" ** "scala-library*.jar") +++ (
      "lib" / "scala-*.jar")
  
  // For continuous redeployment: http://code.google.com/p/simple-build-tool/wiki/WebApplications
  // Use sbt "~prepare-webapp" command for automatic redeployments.
  override def scanDirectories = ( ( temporaryWarPath / "WEB-INF" / "classes" ) +++
                                    ( temporaryWarPath / "WEB-INF" / "lib") ).get.toSeq

  override def compileOptions = super.compileOptions ++ 
    Seq("-g:line", "-deprecation", "-unchecked").map(x => CompileOption(x))

  // The sbt script has an option to set this property:
  override def jettyPort = 
    Integer.parseInt(System.getProperty("jetty.port", super.jettyPort.toString))
}
