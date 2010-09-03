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
import org.joda.time._

@Path("/")
class RestfulDataPublisher extends Logging {
   
  val actorName = "RestfulDataPublisher"
  
  /**
   * Handle a rest request. The following "actions" are supported:
   *   stats:   Calculate and return statistics for financial instruments, subject to the URL options: 
   *              ?start=long&end=long&symbols=i1,i1,...&stats=s1,s2,...
   *            where
   *              "start"   is the starting date time, inclusive (default: earliest available).
   *              "end"     is the ending date time, inclusive (default: latest available).
   *              "symbols" is the comma-separated list of instruments by "symbol" to analyze (default: all available).
   *              "stats"   is the comma-separated list of statistics to calculate (default: all available).
   *            The allowed date time formats include milliseconds (Long) and any date time string that can be parsed
   *            by JodaTime.
   *   ping:    Send a "ping" message to each actor and return the responses.
   * @todo: It would be nice to use an HTML websocket to stream results to the browser more dynamically.
   * Consider also Atmosphere 6 and its JQuery plugin as an abstraction that supports
   * websockets, but can degrade to Comet, etc., when used with a browser-server combination that doesn't support
   * websockets (@see http://jfarcand.wordpress.com/2010/06/15/using-atmospheres-jquery-plug-in-to-build-applicationsupporting-both-websocket-and-comet/).
   */
  @GET
  @Path("{action}")
  @Produces(Array("application/json"))
  def restRequest(
      @PathParam("action") action: String, 
      @DefaultValue("0")  @QueryParam("start")   start: String,
      @DefaultValue("-1") @QueryParam("end")     end: String,
      @DefaultValue("")   @QueryParam("symbols") instruments: String,
      @DefaultValue("")   @QueryParam("stats")   stats: String): String = 
    action match {
      case "ping" => 
        try {
          log.info("Pinging!!")
          val futures = instrumentAnalysisServerSupervisors map { _ !!! Pair("ping", "You there??") }
          Futures.awaitAll(futures)
          val replyMessage = """{"ping replies": """ + handlePingReplies(futures) + "}"
          log.info("Ping replies: " + replyMessage)
          replyMessage
        } catch {
          case fte: FutureTimeoutException =>
            """{"error": "Actors timed out (""" + fte.getMessage + ").\"}"
        }
        
      case "stats" =>
        log.debug("Requesting statistics for instruments, stats, start, end = "+instruments+", "+stats+", "+start+", "+end)
        getAllDataFor(instruments, stats, start, end)
        
      case x => """{"error": "Unrecognized 'action': """ + action + "\"}"
    }
    
  // Scoped to the "rest" package so tests can call it directly (bypassing actor logic...)
  protected[rest] def getAllDataFor(instruments: String, stats: String, start: String, end: String): String = 
    try {
      val allCriteria = CriteriaMap().withInstruments(instruments).withStatistics(stats).withStart(start).withEnd(end)
      val futures = sendAndReturnFutures(allCriteria) 
      Futures.awaitAll(futures)    // wait for all of them to reply.
      val messageForNone = "No data available!"
      val jsons = for {
        future <- futures
      } yield futureToJSON(future, messageForNone)
      toJSON(jsons)
    } catch {
      case iae: IllegalArgumentException => 
        """{"error": "One or both date time arguments are invalid: start = """ + start + ", end = " + end + ".\"}"
      case fte: FutureTimeoutException =>
        """{"error": "Actors timed out (""" + fte.getMessage + ").\"}"
      case awsee: AkkaWebSampleExerciseException =>
        """{"error": "Invalid input: """ + awsee.getMessage + ".\"}"
    }
  
  protected def futureToJSON(future: Future[_], messageForNone: String) = future.result match {
    case Some(result) => result.toString
    case None => "{\"error\": \"" + messageForNone + "\"}"
  }

  protected def toJSON(jsons: Iterable[String]) = jsons.size match {
    case 0 => "{\"error\": \"No data servers appear to be available.\"}"
    case _ => jsons.reduceLeft(_ + ", " + _)
  }
  
  protected def instrumentAnalysisServerSupervisors =
    ActorRegistry.actorsFor(classOf[InstrumentAnalysisServerSupervisor]).toList
  
  protected def handlePingReplies(futures: Iterable[Future[_]]) = {
    val messageForNone = "failed!"
    val replies = for { future <- futures } yield futureToJSON(future, messageForNone)
    replies.size match {
      case 0 => "[]"
      case _ => "[" + (replies reduceLeft (_ + ", " + _)) + "]"
    }
  }  
  
  // Extracted this logic into a method so it can be overridden in a "test double".
  protected def sendAndReturnFutures(criteria: CriteriaMap) = 
    instrumentAnalysisServerSupervisors map { _ !!! CalculateStatistics(criteria) }
}
