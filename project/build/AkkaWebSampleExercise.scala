import sbt._
import sbt.CompileOrder._

// Portions adapted from http://github.com/mgutz/sbt-console-template and from Akka's project structure.

class AkkaWebSampleExercise(info: ProjectInfo) extends DefaultWebProject(info)  {

	object Repositories {
    lazy val AkkaRepo             = MavenRepository("Akka Repository", "http://scalablesolutions.se/akka/repository")
    lazy val CodehausRepo         = MavenRepository("Codehaus Repo", "http://repository.codehaus.org")
    lazy val CasbahRepo           = MavenRepository("Casbah Repo", "http://repo.bumnetworks.com/releases/")    
    lazy val EmbeddedRepo         = MavenRepository("Embedded Repo", (info.projectPath / "embedded-repo").asURL.toString)
    lazy val FusesourceSnapshotRepo = MavenRepository("Fusesource Snapshots", "http://repo.fusesource.com/nexus/content/repositories/snapshots")
    lazy val GuiceyFruitRepo      = MavenRepository("GuiceyFruit Repo", "http://guiceyfruit.googlecode.com/svn/repo/releases/")
    lazy val JBossRepo            = MavenRepository("JBoss Repo", "http://repository.jboss.org/nexus/content/groups/public/")
    lazy val JavaNetRepo          = MavenRepository("java.net Repo", "http://download.java.net/maven/2")
    lazy val SonatypeSnapshotRepo = MavenRepository("Sonatype OSS Repo", "http://oss.sonatype.org/content/repositories/releases")
    lazy val SunJDMKRepo          = MavenRepository("Sun JDMK Repo", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo")
    lazy val Maven2Repo           = MavenRepository("Maven2 Repo", "http://repo2.maven.org/maven2/")
  }
  
  // Old list: Delete when we know it's no longer necessary.
  // override def repositories = Set(
  //    "Atmosphere" at "http://oss.sonatype.org/content/repositories/snapshots/",
  //   "Sun JDMK Repo" at "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo/",
  //   "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots",
  //    "Multiverse Releases" at "http://multiverse.googlecode.com/svn/maven-repository/releases/",
  //    "GuiceyFruit" at "http://guiceyfruit.googlecode.com/svn/repo/releases/",
  //    "DataBinder" at "http://databinder.net/repo",
  //    "Configgy" at "http://www.lag.net/repo",
  //    "Akka Maven Repository" at "http://scalablesolutions.se/akka/repository",
  //    "Java.Net" at "http://download.java.net/maven/2",
  //    "Scala Tools" at "http://scala-tools.org/repo-releases",
  //   "google" at "http://undercover.googlecode.com/svn/maven/repository")


  // -------------------------------------------------------------------------------------------------------------------
  // ModuleConfigurations
  // Every dependency that cannot be resolved from the built-in repositories (Maven Central and Scala Tools Releases)
  // must be resolved from a ModuleConfiguration. This will result in a significant acceleration of the update action.
  // Therefore, if repositories are defined, this must happen as def, not as val.
  // -------------------------------------------------------------------------------------------------------------------

  import Repositories._
  lazy val atmosphereModuleConfig  = ModuleConfiguration("org.atmosphere",  SonatypeSnapshotRepo)
  lazy val casbahModuleConfig      = ModuleConfiguration("com.novus",  CasbahRepo)
  lazy val grizzlyModuleConfig     = ModuleConfiguration("com.sun.grizzly", JavaNetRepo)
  lazy val guiceyFruitModuleConfig = ModuleConfiguration("org.guiceyfruit", GuiceyFruitRepo)
  lazy val liftModuleConfig        = ModuleConfiguration("net.liftweb",     ScalaToolsReleases)
  lazy val multiverseModuleConfig  = ModuleConfiguration("org.multiverse",  CodehausRepo)
  lazy val nettyModuleConfig       = ModuleConfiguration("org.jboss.netty", JBossRepo)
  lazy val scalaTestModuleConfig   = ModuleConfiguration("org.scalatest",   ScalaToolsSnapshots)

  override def repositories = Set(
    AkkaRepo, CasbahRepo, CodehausRepo, EmbeddedRepo, FusesourceSnapshotRepo, GuiceyFruitRepo, 
    JBossRepo, JavaNetRepo, SonatypeSnapshotRepo, SunJDMKRepo)
    
  // -------------------------------------------------------------------------------------------------------------------
  // Versions
  // -------------------------------------------------------------------------------------------------------------------

  lazy val AKKA_VERSION          = "0.10"
  lazy val ATMO_VERSION          = "0.6.1"
  lazy val CAMEL_VERSION         = "2.4.0"
  lazy val CASBAH_VERSION        = "1.0.8.5"
  lazy val JERSEY_VERSION        = "1.2"
  lazy val LIFT_VERSION          = "2.1-M1"
  lazy val MULTIVERSE_VERSION    = "0.6"
  lazy val SCALATEST_VERSION     = "1.2-for-scala-2.8.0.final-SNAPSHOT"

  lazy val ECLIPSE_JETTY_VERSION = "7.1.6.v20100715" //  "7.0.2.v20100331"
  lazy val MORTBAY_JETTY_VERSION = "6.1.22"  


  object Dependencies {

    // Compile

    lazy val akkaCore         = "se.scalablesolutions.akka" % "akka-core_2.8.0"        % AKKA_VERSION % "compile"
    lazy val akkaCamel        = "se.scalablesolutions.akka" % "akka-camel_2.8.0"       % AKKA_VERSION % "compile"
    lazy val akkaHttp         = "se.scalablesolutions.akka" % "akka-http_2.8.0"        % AKKA_VERSION % "compile"
    lazy val akkaKernel       = "se.scalablesolutions.akka" % "akka-kernel_2.8.0"      % AKKA_VERSION % "compile"
    lazy val akkaPersistence  = "se.scalablesolutions.akka" % "akka-persistence_2.8.0" % AKKA_VERSION % "compile"

    lazy val atmo         = "org.atmosphere" % "atmosphere-annotations"     % ATMO_VERSION % "compile"
    lazy val atmoRuntime  = "org.atmosphere" % "atmosphere-runtime"         % ATMO_VERSION % "compile"

    lazy val casbah   = "com.novus" % "casbah_2.8.0" % CASBAH_VERSION % "compile"

    lazy val configgy = "net.lag" % "configgy" % "2.8.0-1.5.5" % "compile"

    lazy val liftJSON = "net.liftweb" % "lift-json_2.8.0" % LIFT_VERSION % "compile"

    lazy val mongo = "org.mongodb" % "mongo-java-driver" % "2.0" % "compile"

    lazy val mongoScalaDriver = "com.osinka" % "mongo-scala-driver_2.8.0" % "0.8.3" % "compile"

    lazy val jersey         = "com.sun.jersey"          % "jersey-core"   % JERSEY_VERSION % "compile"
    lazy val jersey_json    = "com.sun.jersey"          % "jersey-json"   % JERSEY_VERSION % "compile"
    lazy val jersey_server  = "com.sun.jersey"          % "jersey-server" % JERSEY_VERSION % "compile"
    lazy val jersey_contrib = "com.sun.jersey.contribs" % "jersey-scala"  % JERSEY_VERSION % "compile"

    // Test

    // lazy val jettyServer    = "org.eclipse.jetty"      % "jetty-distribution"  % ECLIPSE_JETTY_VERSION  % "test"
    lazy val jettyBase      = "org.mortbay.jetty"      % "jetty"           % MORTBAY_JETTY_VERSION   % "test"
    // lazy val jettyServer    = "org.mortbay.jetty"      % "jetty-server"    % MORTBAY_JETTY_VERSION   % "test"
    // lazy val jettyWebApp    = "org.mortbay.jetty"      % "jetty-webapp"    % MORTBAY_JETTY_VERSION   % "test"
    // lazy val jettyServlets  = "org.mortbay.jetty"      % "jetty-servlets"  % MORTBAY_JETTY_VERSION   % "test"

    lazy val scalatest      = "org.scalatest"          % "scalatest"           % SCALATEST_VERSION % "test"
    lazy val junit          = "junit"                  % "junit"               % "4.5"             % "test"
  }

  import Dependencies._
  override def libraryDependencies = Set(
    akkaCore, akkaCamel, akkaHttp, akkaKernel, 
    atmo, atmoRuntime, 
    // casbah, 
    configgy, 
    liftJSON, 
    mongo, mongoScalaDriver,
    jettyBase, //jettyServer, jettyWebApp, jettyServlets,
    jersey, jersey_json, jersey_server, jersey_contrib,
    scalatest, junit)

	/* Embedded Jetty web server */
  // "org.eclipse.jetty"  % "jetty-server"   % JETTY_VERSION % "test",
  // "org.eclipse.jetty"  % "jetty-webapp"   % JETTY_VERSION % "test",
  // "org.eclipse.jetty"  % "jetty-servlets" % JETTY_VERSION % "test",

  
  // For continuous redeployment: http://code.google.com/p/simple-build-tool/wiki/WebApplications
  // Use sbt "~prepare-webapp" command for automatic redeployments.
  override def scanDirectories = ( ( temporaryWarPath / "WEB-INF" / "classes" ) +++
                                   ( temporaryWarPath / "WEB-INF" / "lib") ).get.toSeq

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-unchecked",
        "-Xmigration",
        "-Xcheckinit",
        "-Xwarninit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))
  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // The sbt script has an option to set this property:
  override def jettyPort = 
    Integer.parseInt(System.getProperty("jetty.port", super.jettyPort.toString))
}
