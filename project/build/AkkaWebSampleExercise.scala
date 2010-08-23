import sbt._

// Portions adapted from http://github.com/mgutz/sbt-console-template

class AkkaWebSampleExercise(info: ProjectInfo) extends DefaultWebProject(info)  {

	override def repositories = Set(
	  "Atmosphere" at "http://oss.sonatype.org/content/repositories/snapshots/",
    "Sun JDMK Repo" at "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo/",
    "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots",
    // "jBoss" at "http://repository.jboss.org/maven2",
    // "JBoss2" at "http://repository.jboss.org/nexus/content/groups/public/",
		"Multiverse Releases" at "http://multiverse.googlecode.com/svn/maven-repository/releases/",
		"GuiceyFruit" at "http://guiceyfruit.googlecode.com/svn/repo/releases/",
		"DataBinder" at "http://databinder.net/repo",
		"Configgy" at "http://www.lag.net/repo",
		"Akka Maven Repository" at "http://scalablesolutions.se/akka/repository",
		"Java.Net" at "http://download.java.net/maven/2",
		"Scala Tools" at "http://scala-tools.org/repo-releases",
    "google" at "http://undercover.googlecode.com/svn/maven/repository")

  val SCALATEST_VERSION = "1.2-for-scala-2.8.0.RC3-SNAPSHOT"
  val JETTY_VERSION     = "7.0.2.v20100331"

	override def libraryDependencies = Set(

    "net.lag" % "configgy" % "2.8.0.RC3-1.5.2-SNAPSHOT" % "compile",
  
    "org.scalatest" % "scalatest" % SCALATEST_VERSION % "test",

		/* Embedded Jetty web server */
    "org.eclipse.jetty"  % "jetty-server"   % JETTY_VERSION % "test",
    "org.eclipse.jetty"  % "jetty-webapp"   % JETTY_VERSION % "test",
    "org.eclipse.jetty"  % "jetty-servlets" % JETTY_VERSION % "test",

		/* akka dependencies */
    "se.scalablesolutions.akka" % "akka-core_2.8.0.RC3"        % "0.9" % "compile",
    "se.scalablesolutions.akka" % "akka-camel_2.8.0.RC3"       % "0.9" % "compile",
    "se.scalablesolutions.akka" % "akka-http_2.8.0.RC3"        % "0.9" % "compile",
    "se.scalablesolutions.akka" % "akka-kernel_2.8.0.RC3"      % "0.9" % "compile",
    "se.scalablesolutions.akka" % "akka-persistence_2.8.0.RC3" % "0.9" % "compile",

    "net.liftweb" % "lift-json"   % "2.0-scala280-SNAPSHOT" % "compile",

    "com.osinka" % "mongo-scala-driver_2.8.0.RC3" % "0.8.0" % "compile")
  
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
