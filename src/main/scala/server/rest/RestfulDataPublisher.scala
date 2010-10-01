package org.chicagoscala.awse.server.rest
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.server.finance._
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.util._
import org.chicagoscala.awse.util.json._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.dispatch.{Future, Futures, FutureTimeoutException}
import se.scalablesolutions.akka.util.Logging
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import javax.ws.rs._
import org.joda.time._

case object NoWorkersAvailable extends RuntimeException("No worker servers appear to be available!")

@Path("/")
class RestfulDataPublisher extends Logging {
   
  val actorName = "RestfulDataPublisher"
  
  /**
   * Handle a rest request. The following "actions" are supported:
   *   stats:   Calculate and return statistics for financial instruments, subject to the URL options: 
   *              ?start=long&end=long&symbols=i1,i1,...&stats=s1,s2,...
   *            where
   *              "start"   is the starting date, inclusive (default: earliest available).
   *              "end"     is the ending date, inclusive (default: latest available).
   *              "symbols" is the comma-separated list of instruments by "symbol" to analyze (default: all available).
   *              "stats"   is the comma-separated list of statistics to calculate (default: all available).
   *            The allowed date time formats include milliseconds (Long) and any date time string that can be parsed
   *            by JodaTime.
   *   list_instruments: Return a list of the symbols of all the financial instruments. 
   *   ping:    Send a "ping" message to each actor and return the responses.
   *   <other>  If any other message is received, an error response is returned.
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
        log.info("Pinging actors...")
        val results = for {
          supervisor <- instrumentAnalysisServerSupervisors
          result <- supervisor !! Ping("you there??")
        } yield result
        val result = compact(render(JSONMap.toJValue(Map("pong" -> results))))
        log.info("ping result: "+result)
        result

      case "list_instruments" =>
        log.debug("Requesting a list of all instruments")
        getAllInstruments(instruments)

      case "stats" =>
        log.debug("Requesting statistics for instruments, stats, start, end = "+instruments+", "+stats+", "+start+", "+end)
        getAllDataFor(instruments, stats, start, end)
        
      case x => """{"error": "Unrecognized 'action': """ + action + "\"}"
    }
    
  // Scoped to the "rest" package so tests can call it directly (bypassing actor logic...)
  protected[rest] def getAllDataFor(instruments: String, stats: String, start: String, end: String): String = 
    try {
      val allCriteria = CriteriaMap().withInstruments(instruments).withStatistics(stats).withStart(start).withEnd(end)
      val results = getStatsFromInstrumentAnalysisServerSupervisors(CalculateStatistics(allCriteria))
      val result = compact(render(JSONMap.toJValue(Map("financial-data" -> results))))
      val length = if (result.length > 100) 100 else result.length
      log.info("financial data result = "+result.substring(0, length)+"...")
      result
    } catch {
      case NoWorkersAvailable =>
        makeErrorString("", NoWorkersAvailable, instruments, stats, start, end)
      case iae: CriteriaMap.InvalidTimeString => 
        makeErrorString("", iae, instruments, stats, start, end)
      case fte: FutureTimeoutException =>
        makeErrorString("Actors timed out", fte, instruments, stats, start, end)
      case awsee: AkkaWebSampleExerciseException =>
        makeErrorString("Invalid input", awsee, instruments, stats, start, end)
      case th: Throwable => 
        makeErrorString("An unexpected problem occurred during processing the request", 
          th, instruments, stats, start, end)
    }
    
  protected[rest] def getAllInstruments(instruments: String) = 
    try {
      // Hack! Just grab the first and last letter.
			val symbolRange = instruments.trim match {
        case "" => 'A' to 'Z'
        case s  => s.length match {
          case 1 => s.charAt(0).toUpper to 'Z'
          case n => s.charAt(0).toUpper to s.charAt(n-1).toUpper
        }
      }
      val results = getStatsFromInstrumentAnalysisServerSupervisors(GetInstrumentList(symbolRange, "stock_symbol"))
      log.info("Rest: instruments results: "+results)
      val result = compact(render(JSONMap.toJValue(
          Map("instrument-list" -> results, "instrument_symbols_key" -> "stock_symbol"))))
      val length = if (result.length > 200) 200 else result.length
      log.info("instrument list result = "+result.substring(0,length)+"...")
      result
    } catch {
      case NoWorkersAvailable =>
        makeAllInstrumentsErrorString(instruments, "", NoWorkersAvailable)
      case iae: CriteriaMap.InvalidTimeString => 
        makeAllInstrumentsErrorString(instruments, "", iae)
      case fte: FutureTimeoutException =>
        makeAllInstrumentsErrorString(instruments, "Actors timed out", fte)
      case awsee: AkkaWebSampleExerciseException =>
        makeAllInstrumentsErrorString(instruments, "Invalid input", awsee)
      case th: Throwable => 
        makeAllInstrumentsErrorString(instruments, "An unexpected problem occurred during processing the request", th)
    }
    
  
  protected def getStatsFromInstrumentAnalysisServerSupervisors(message: InstrumentCalculationMessages): JValue =
    instrumentAnalysisServerSupervisors match {
      case Nil => error(NoWorkersAvailable)
      case supervisors => supervisors map { supervisor =>
        (supervisor !! message) match {
          case Some(x) => JSONMap.toJValue(x)
          case None => JNothing
        }
      } reduceLeft (_ ++ _)
    }
    
  protected def instrumentAnalysisServerSupervisors =
    ActorRegistry.actorsFor(classOf[InstrumentAnalysisServerSupervisor]).toList
  
  protected def makeErrorString(message: String, th: Throwable, 
      instruments: String, stats: String, start: String, end: String) =
    "{\"error\": \"" + (if (message.length > 0) (message + ". ") else "") + th.getMessage + ". Investment instruments = '" + 
      instruments + "', statistics = '" + stats + "', start = '" + start + "', end = '" + end + "'.\"}"

  protected def makeAllInstrumentsErrorString(instruments: String, message: String, th: Throwable) =
    "{\"error\": \"Getting instruments for " + instruments + " failed. " + (if (message.length > 0) (message + ". ") else "") + th.getMessage + "\"}"
}
