package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.util.json.JSONMap._
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence.mongodb._
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.domain.finance.FinanceJSONConverter._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import org.chicagoscala.awse.util._
import net.lag.logging.Level

sealed trait InstrumentCalculationMessages

case class CalculateStatistics(criteria: CriteriaMap) extends InstrumentCalculationMessages
      
/**
 * InstrumentAnalysisServer is a worker that calculates (or simply fetches...) statistics for financial instruments.
 * It reads data from and writes results to a DataStorageServer, which it supervises.
 * It is parameterized by the type of the date time values used as timestamps.
 * TODO: The relationship and management of these servers, DataStorageServers and DataStores is
 * convoluted and messy. Refactor...
 */
class InstrumentAnalysisServer(val service: String, dataStorageServer: ActorRef) extends Transactor 
    with ActorUtil with ActorFactory with PingHandler with Logging {
  
  val actorName = "InstrumentAnalysisServer("+service+")"
  
  manageNewActor(dataStorageServer)
  
  /**
   * The message handler calls the "pingHandler" first. If it doesn't match on the
   * message (because it is a PartialFunction), then its own "defaultHandler" is tried,
   * and finally "unrecognizedMessageHandler" (from the ActorUtil trait) is tried.
   */
  def receive = pingHandler orElse defaultHandler orElse unrecognizedMessageHandler

  def defaultHandler: PartialFunction[Any, Unit] = {
    case CalculateStatistics(criteria) => self.reply(helper.calculateStatistics(criteria))
  }
  
  override protected def subordinatesToPing: List[ActorRef] = List(dataStorageServer)
  
  val helper = new InstrumentAnalysisServerHelper(dataStorageServer)
}

/**
 * A separate helper so we can decouple (most of) the actor-specific code 
 * and the logic it performs, primarily when testing this code.
 @ param dataStorageServer a by-name parameter so it is lazy!
 */
class InstrumentAnalysisServerHelper(dataStorageServer: => ActorRef) {
  
  def calculateStatistics(criteria: CriteriaMap): JValue = criteria match {
    case CriteriaMap(instruments, statistics, start, end) => 
       fetchPrices(instruments, statistics, start, end)
    case _ =>
      Pair("error", "Invalid criteria: " + criteria)
  }

  /**
   * Fetch the instrument prices between the time range. Must make a synchronous call to the data store server
   * because clients calling this actor need a synchronous response.
   * TODO: Ignores the instruments criteria. Fix!
   */
  protected def fetchPrices(
        instruments: List[Instrument], statistics: List[InstrumentStatistic], 
        start: DateTime, end: DateTime): JValue = {
    (dataStorageServer !! Get(("start" -> start) ~ ("end" -> end))) match {
      case None => 
        Pair("warning", "Nothing returned for query (start, end) = (" + start + ", " + end + ")")
      case Some(result) => 
        formatPriceResults(filter(result), instruments, statistics, start, end)
    }
  }
  
  /**
   * A "hook" method that could be used to filter by instrument (and maybe statistics) criteria. 
   * However, in general, it would be better to filter in the DB query itself!
   */
  protected def filter(json: JValue): JValue = json
  
  // Public visibility, for testing purposes.
  def formatPriceResults(
      json: JValue, instruments: List[Instrument], statistics: List[InstrumentStatistic], start: DateTime, end: DateTime): JValue = {
    val results = json match {
      case JNothing => toJValue(Nil)  // Use an empty array as the result
      case x => x
    }
    val fullResults = toJValue(Map("criteria" -> toNiceFormat(instruments, statistics, start, end), "results" -> results))
    fullResults
  }
  
  /** Extract and format the data so it's more convenient when returned to the UI. */
  protected def toNiceFormat(instruments: List[Instrument], statistics: List[InstrumentStatistic], start: DateTime, end: DateTime): Map[String, Any] = 
    Map(
      "instruments" -> Instrument.toSymbolNames(instruments),
      "statistics"  -> statistics,
      "start"       -> start,
      "end"         -> end)
}
