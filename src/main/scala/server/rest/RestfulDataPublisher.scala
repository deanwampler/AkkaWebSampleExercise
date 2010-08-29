package org.chicagoscala.awse.server.rest
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.server.finance._
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.util._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.dispatch.{Future, Futures, FutureTimeoutException}
import se.scalablesolutions.akka.util.Logging
import javax.ws.rs._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import org.joda.time._

@Path("/")
class RestfulDataPublisher extends Logging {
   
  val actorName = "RestfulDataPublisher"
  
  def dataStorageServers = ActorRegistry.actorsFor(classOf[DataStorageServer])
  def instrumentAnalysisServerSupervisors = ActorRegistry.actorsFor(classOf[InstrumentAnalysisServerSupervisor])
  def instrumentAnalysisServers = ActorRegistry.actorsFor(classOf[InstrumentAnalysisServer])
  
  /**
   * Handle a rest request. The following "actions" are supported:
   *   start:   Start calculating statistics for the financial instruments, subject to the URL options: 
   *              ?startat=long&upto=long&instruments=i1,i1,...&stats=s1,s2,...
   *            where "startat" is the starting date, inclusive (default: earliest available),
   *            where "upto" is the ending date, exclusive (default: latest available),
   *            where "syms" (symbols) is the comma-separated list of instruments by "symbol" to analyze (default: all available), and
   *            where "stats" is the comma-separated list of statistics to calculate (default: all available).
   *   stop:    Stop calculating statistics. The calculation stops automatically once the calculations requested with
   *            "start" have completed.
   *   stats:   Return the calculated statistics subject to the same URL options described for "start". If no statistics
   *            have been calculated for some or all of the requested options, then empty results will be returned.
   *   ping:    Send a "ping" message to each actor and return their replies.
   * @todo: It would simplify both the web code and this code to use a websocket to stream results to the browser 
   * as they are completed. Consider also Atmosphere 6 and its JQuery plugin as an abstraction that supports
   * websockets, but can degrade to Comet, etc., when used with a browser-server combination that doesn't support
   * websockets (@see http://jfarcand.wordpress.com/2010/06/15/using-atmospheres-jquery-plug-in-to-build-applicationsupporting-both-websocket-and-comet/).
   */
  @GET
  @Path("{action}")
  @Produces(Array("application/json"))
  def restRequest(
      @PathParam("action") action: String, 
      @DefaultValue("0")  @QueryParam("startingAt") startingAt: Long,
      @DefaultValue("-1") @QueryParam("upTo")    upTo: Long,
      @DefaultValue("")   @QueryParam("syms")    instruments: String,
      @DefaultValue("")   @QueryParam("stats")   stats: String): String = 
    action match {
      case "ping" => 
        try {
          log.info("Pinging!!")
          val futures = (instrumentAnalysisServerSupervisors ++ instrumentAnalysisServers ++ dataStorageServers) map {
            _ !!! Pair("ping", "You there??")
          }
          Futures.awaitAll(futures)
          val replyMessage = """{"ping replies": """ + handlePingReplies(futures) + "}"
          log.info("Ping replies: "+replyMessage)
          replyMessage
        } catch {
          case fte: FutureTimeoutException =>
            """{"error": "Actors timed out (""" + fte.getMessage + ").\"}"
        }
        
      case "start" =>
        log.info("Starting statistics calculations")
        ActorRegistry.actorsFor(classOf[InstrumentAnalysisServerSupervisor]) foreach { 
          _ ! CalculateStatistics(makeCriteriaFrom(instruments, stats, startingAt, upTo))
        }
        """{"message": "Starting statistics calculations"}"""
      
      case "stop" =>
        log.info("Stopping calculations")
        ActorRegistry.actorsFor(classOf[InstrumentAnalysisServerSupervisor]) foreach { _ ! StopCalculating }
        """{"message": "Stopping calculations"}"""

      case "stats" =>
        log.debug("Requesting statistics for instruments, stats, startingAt, upTo = "+instruments+", "+stats+", "+startingAt+", "+upTo)
        getAllDataFor(instruments, stats, startingAt, upTo)
        
      case x => """{"error": "Unrecognized 'action': """ + action + "\"}"
    }
    
  // Scoped to rest package so tests can call it directly (bypassing actor logic...)
  protected[rest] def getAllDataFor(instruments: String, stats: String, startingAt: Long, upTo: Long): String = {
    val dsServers = dataStorageServers
    if (dsServers.size == 0) {
      val message = "RestfulDataPublisher: No DataStorageServers! (normal at startup)"
      log.warning(message)
      "{\"warn\": \"" + message + "\"}"
    } else {
      val futures = dataStorageServers map { server =>
        // fire messages to all data servers...
        println("server!")
        server !!! Get(makeCriteriaFrom(instruments, stats, startingAt, upTo))
      }
      try {
        Futures.awaitAll(futures)        // ... and wait for all of them to reply.
        val messageForNone = "No data available for start-end times = "+startingAt+", "+computeUpTo(upTo)
        val jsons = for {
          future <- futures
        } yield futureToJSON(future, messageForNone)
        println("jsons: "+jsons)
        toJSON(jsons)
      } catch {
        case fte: FutureTimeoutException =>
          """{"error": "Actors timed out (""" + fte.getMessage + ").\"}"
      }
    }
  }
  
  protected def futureToJSON(future: Future[_], messageForNone: String) = future.result match {
    case Some(result) => result.toString
    case None => "{\"error\": \"" + messageForNone + "\"}"
  }

  protected def toJSON(jsons: Iterable[String]) = jsons.size match {
    case 0 => "{\"error\": \"No data servers appear to be available.\"}"
    case _ => "[" + jsons.reduceLeft(_ + ", " + _) + "]"
  }
  
  protected def handlePingReplies(futures: Iterable[Future[_]]) = {
    val messageForNone = "failed!"
    val replies = for { future <- futures } yield futureToJSON(future, messageForNone)
    replies.size match {
      case 0 => "[]"
      case _ => "[" + (replies reduceLeft (_ + ", " + _)) + "]"
    }
  }  
  
  protected def makeCriteriaFrom(instruments: String, statistics: String, startingAt: Long, upTo: Long) = 
    Map(
      "instruments" -> Instrument.makeInstrumentsList(instruments), 
      "statistics"  -> InstrumentStatistic.makeStatisticsList(statistics), 
      "startingAt"     -> new DateTime(startingAt), 
      "upTo"        -> computeUpTo(upTo))
  
  protected def computeUpTo(candidateUpTo: Long) = 
    if (candidateUpTo > 0) new DateTime(candidateUpTo) else new DateTime
}
