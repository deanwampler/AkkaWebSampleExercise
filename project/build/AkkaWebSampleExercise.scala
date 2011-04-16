import sbt._
import sbt.CompileOrder._
import reaktor.scct.ScctProject
import de.element34.sbteclipsify._

// Portions adapted from http://github.com/mgutz/sbt-console-template and from Akka's project structure
// and Victor Klang's blog post on speeding up sbt update times (http://klangism.tumblr.com/post/2141977562/hardcore-pom).

class AkkaWebSampleExercise(info: ProjectInfo) extends DefaultWebProject(info) with ScctProject with AkkaProject with IdeaProject with Eclipsify with Exec {

	object Repositories {
    lazy val EmbeddedRepo         = MavenRepository("Embedded Repo", (info.projectPath / "embedded-repo").asURL.toString)
    lazy val LocalMavenRepo       = MavenRepository("Local Maven Repo", (Path.userHome / ".m2" / "repository").asURL.toString)
    lazy val AkkaRepo             = MavenRepository("Akka Repository", "http://akka.io/repository")
    lazy val ScalaToolsRepo       = MavenRepository("Scala Tools Repo", "http://nexus.scala-tools.org/content/repositories/hosted")
    lazy val DavScalaToolsRepo    = MavenRepository("Dav Scala Tools", "http://dav.scala-tools.org/repo-releases/")
    lazy val CodehausRepo         = MavenRepository("Codehaus Repo", "http://repository.codehaus.org")
    lazy val FusesourceSnapshotRepo = MavenRepository("Fusesource Snapshots", "http://repo.fusesource.com/nexus/content/repositories/snapshots")
    lazy val GuiceyFruitRepo      = MavenRepository("GuiceyFruit Repo", "http://guiceyfruit.googlecode.com/svn/repo/releases/")
    lazy val JBossRepo            = MavenRepository("JBoss Repo", "http://repository.jboss.org/nexus/content/groups/public/")
    lazy val JavaNetRepo          = MavenRepository("java.net Repo", "http://download.java.net/maven/2")
    lazy val SonatypeSnapshotRepo = MavenRepository("Sonatype OSS Repo", "http://oss.sonatype.org/content/repositories/releases")
    lazy val GlassfishRepo        = MavenRepository("Glassfish Repo", "http://download.java.net/maven/glassfish")
    lazy val SunJDMKRepo          = MavenRepository("Sun JDMK Repo", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo")
    lazy val Maven2Repo           = MavenRepository("Maven2 Repo", "http://repo2.maven.org/maven2/")
  }

  // -------------------------------------------------------------------------------------------------------------------
  // ModuleConfigurations
  // Every dependency that cannot be resolved from the built-in repositories (Maven Central and Scala Tools Releases)
  // must be resolved from a ModuleConfiguration. This will result in a significant acceleration of the update action.
  // Therefore, if repositories are defined, this must happen as def, not as val.
  // -------------------------------------------------------------------------------------------------------------------

  // akkaActor is already defined by the akk sbt plugin.
  // val akkaActor      = akkaModule("actor")
  val akkaStm        = akkaModule("stm")
  val akkaTypedActor = akkaModule("typed-actor")
  val akkaRemote     = akkaModule("remote")
  val akkaHttp       = akkaModule("http")
  val akkaAmqp       = akkaModule("amqp")
  val akkaCamel      = akkaModule("camel")
  val akkaJta        = akkaModule("jta")
  val akkaKernel     = akkaModule("kernel")
  val akkaModules    = akkaModule("modules")
  // val akkaCassandra  = akkaModule("persistence-cassandra")
  // val akkaMongo      = akkaModule("persistence-mongo")
  // val akkaRedis      = akkaModule("persistence-redis")
  // val akkaSpring     = akkaModule("spring")
  
  import Repositories._
  lazy val embeddedRepo            = EmbeddedRepo   // This is the only exception, because the embedded repo is fast!
  lazy val localMavenRepo          = LocalMavenRepo // Second exception, also fast! ;-)
  lazy val jettyModuleConfig       = ModuleConfiguration("org.eclipse.jetty", sbt.DefaultMavenRepository)
  lazy val databinderModuleConfig  = ModuleConfiguration("net.databinder",  ScalaToolsRepo)
  lazy val liftModuleConfig        = ModuleConfiguration("net.liftweb",     ScalaToolsReleases)
  lazy val scalaTestModuleConfig   = ModuleConfiguration("org.scalatest",   ScalaToolsRepo)

  override def repositories = Set(
    AkkaRepo, DavScalaToolsRepo, CodehausRepo, EmbeddedRepo, FusesourceSnapshotRepo, 
    GuiceyFruitRepo, JBossRepo, JavaNetRepo, SonatypeSnapshotRepo, SunJDMKRepo)
    
  // -------------------------------------------------------------------------------------------------------------------
  // Versions
  // -------------------------------------------------------------------------------------------------------------------

  lazy val AKKA_VERSION          = "1.1-M1"
  lazy val ATMO_VERSION          = "0.6.1"
  lazy val CAMEL_VERSION         = "2.4.0"
  lazy val CASBAH_VERSION        = "2.0.2"
  lazy val JERSEY_VERSION        = "1.2"
  lazy val LIFT_VERSION          = "2.1-M1"
  lazy val MULTIVERSE_VERSION    = "0.6.1"
  lazy val SCALATEST_VERSION     = "1.2"

  lazy val ECLIPSE_JETTY_VERSION = "7.1.6.v20100715" //  "7.0.2.v20100331"
  lazy val MORTBAY_JETTY_VERSION = "6.1.22"  
  lazy val DISPATH_VERSION       = "0.7.4"


  object Dependencies {

    // Compile

    lazy val dispatch_futures = "net.databinder" % "dispatch-futures_2.8.0" % DISPATH_VERSION % "compile"
    lazy val dispatch_http    = "net.databinder" % "dispatch-http_2.8.0"    % DISPATH_VERSION % "compile"
    lazy val dispatch_json    = "net.databinder" % "dispatch-json_2.8.0"    % DISPATH_VERSION % "compile"

    lazy val atmo         = "org.atmosphere" % "atmosphere-annotations"     % ATMO_VERSION % "compile"
    lazy val atmoRuntime  = "org.atmosphere" % "atmosphere-runtime"         % ATMO_VERSION % "compile"

    lazy val casbah   = "com.mongodb.casbah" %% "casbah" % CASBAH_VERSION % "compile"

    lazy val configgy = "net.lag" % "configgy" % "2.8.0-1.5.5" % "compile"

    lazy val liftJSON = "net.liftweb" % "lift-json_2.8.0" % LIFT_VERSION % "compile"

    lazy val mongo = "org.mongodb" % "mongo-java-driver" % "2.0" % "compile"

    // lazy val mongoScalaDriver = "com.osinka" % "mongo-scala-driver_2.8.0" % "0.8.3" % "compile"

    lazy val jersey         = "com.sun.jersey"          % "jersey-core"   % JERSEY_VERSION % "compile"
    lazy val jersey_json    = "com.sun.jersey"          % "jersey-json"   % JERSEY_VERSION % "compile"
    lazy val jersey_server  = "com.sun.jersey"          % "jersey-server" % JERSEY_VERSION % "compile"
    lazy val jersey_contrib = "com.sun.jersey.contribs" % "jersey-scala"  % JERSEY_VERSION % "compile"

    
    // Test

    lazy val jettyBase      = "org.mortbay.jetty"      % "jetty"           % MORTBAY_JETTY_VERSION  % "test"
    lazy val scalatest      = "org.scalatest"          % "scalatest"       % SCALATEST_VERSION      % "test"
  }

  import Dependencies._
  override def libraryDependencies = Set(
    dispatch_futures, dispatch_http, dispatch_json, 
    atmo, atmoRuntime, 
    configgy, 
    liftJSON, 
    mongo, // mongoScalaDriver,
    casbah, 
    jettyBase,
    scalatest) ++
    (Set(akkaStm, akkaActor, akkaTypedActor, akkaCamel, akkaHttp, akkaKernel) map (_ % "compile"))

  
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
        // "-Xfatal-warnings",
        // "-verbose",
        // "-Xstrict-warnings",
        // "-optimise",  // for releases.
        "-encoding", "utf8")
        .map(x => CompileOption(x))
  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // The sbt script has an option to set this property:
  override def jettyPort = 
    Integer.parseInt(System.getProperty("jetty.port", super.jettyPort.toString))
}
