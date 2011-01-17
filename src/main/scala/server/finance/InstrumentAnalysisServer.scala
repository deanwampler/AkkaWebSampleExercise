package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.util.json.JSONMap._
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.domain.finance.FinanceJSONConverter._
import akka.actor._
import akka.actor.Actor._
import akka.util.Logging
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import org.chicagoscala.awse.util._
import net.lag.logging.Level

sealed trait InstrumentCalculationMessages

case class CalculateStatistics(criteria: CriteriaMap) extends InstrumentCalculationMessages
case class GetInstrumentList(symbolFirstLetters: List[Char], keyForInstrumentSymbols: String) 
  extends InstrumentCalculationMessages
      
/**
 * InstrumentAnalysisServer is a worker that calculates (or simply fetches...) statistics for financial instruments.
 * It reads data from and writes results to a DataStorageServer, which it supervises.
 * It is parameterized by the type of the date time values used as timestamps.
 * TODO: The relationship and management of these servers, DataStorageServers and DataStores is
 * convoluted and messy. Refactor...
 */
class InstrumentAnalysisServer(val service: String, pricesDataStorageServer: ActorRef, dividendsDataStorageServer: ActorRef) extends Actor 
    with ActorUtil with ActorFactory with PingHandler with Logging {
  
  def actorName = "InstrumentAnalysisServer("+service+")"
  
  manageNewActor(pricesDataStorageServer)
  manageNewActor(dividendsDataStorageServer)
  override protected def subordinatesToPing: List[ActorRef] = List(pricesDataStorageServer, dividendsDataStorageServer)
  
  /**
   * The message handler calls the "pingHandler" first. If it doesn't match on the
   * message (because it is a PartialFunction), then its own "defaultHandler" is tried,
   * and finally "unrecognizedMessageHandler" (from the ActorUtil trait) is tried.
   */
  def receive = pingHandler orElse defaultHandler orElse unrecognizedMessageHandler

  def defaultHandler: PartialFunction[Any, Unit] = {
    case CalculateStatistics(criteria) => 
      self.reply(helper.calculateStatistics(criteria))
    case GetInstrumentList(symbolFirstLetters, keyForInstrumentSymbols) => 
      self.reply(helper.getInstrumentList(symbolFirstLetters, keyForInstrumentSymbols))
  }  
  
  val helper = new InstrumentAnalysisServerHelper(pricesDataStorageServer, dividendsDataStorageServer)
}

/**
 * A separate helper so we can decouple (most of) the actor-specific code 
 * and the logic it performs, primarily when testing this code.
 @ param dataStorageServer a by-name parameter so it is lazy!
 */
class InstrumentAnalysisServerHelper(pricesDataStorageServer: => ActorRef, dividendsDataStorageServer: => ActorRef) {
  
  def calculateStatistics(criteria: CriteriaMap): JValue = criteria match {
    case CriteriaMap(instruments, statistics, start, end) => fetchPrices(instruments, statistics, start, end)
    case _ => Pair("error", "Invalid criteria: " + criteria)
  }

  /**
   * Fetch the instrument prices between the time range. Must make a synchronous call to the data store server
   * because clients calling this actor need a synchronous response.
   * TODO While you could in principle ask for dividends, we currently don't support them. However, it should be straightforward
   * to add a query of the dividend collections here.
   */
  protected def fetchPrices(
        instruments: List[Instrument], statistics: List[InstrumentStatistic], 
        start: DateTime, end: DateTime): JValue = {
    val (dividends, prices) = statistics partition {
      case d: Dividend => true
      case _ => false
    }
    if (dividends.size > 0) {
      log.error ("You asked for Dividends, but they are currently not supported!")
    }
    (pricesDataStorageServer !! Get(Map("start" -> start, "end" -> end, "stock_symbol" -> Instrument.toSymbolNames(instruments)))) match {
      case None => 
        Pair("warning", "Nothing returned for query (start, end, instruments) = (" + start + ", " + end + ", " + instruments + ")")
      case Some(result) => 
        formatPriceResults(filter(result), instruments, prices, start, end)
    }
  }
  
  /*
   * Only works with one instrument letter, despite the "range" argument.
   * TODO: Currently does no actual filtering, because it assumes that the datastore
   * only contains instruments whose symbols begin with the input Char!
   */
  def getInstrumentList(
        symbolFirstLetters: List[Char], keyForInstrumentSymbols: String): JValue = {
    if (symbolFirstLetters.size != 1) {
      val errMsg = "The input list of of symbol letters does not contain just one item: "+symbolFirstLetters+". (Implementation restriction)"
      log.error(errMsg)
      Pair("error", errMsg)
    } else {
      val letter = symbolFirstLetters.head.toString
      (pricesDataStorageServer !! Get(Map("instrument_list" -> letter, "instrument_symbols_key" -> keyForInstrumentSymbols))) match {
        case None         => Pair("warning", "Nothing returned for instrument list in list "+symbolFirstLetters)
        case Some(result) => result match {
          case jv:JValue => jv + Pair("key", letter)
          case _         => log.error("Expected a JValue to be returned by pricesDataStorageServer!"); result
        }
      }
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
