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

class DataStorageNotAvailable(service: String) extends RuntimeException(
  "Could not get a DataStorageServer for "+service)
  
/**
 * InstrumentAnalysisServer is a worker that calculates (or simply fetches...) statistics for financial instruments.
 * It reads data from and writes results to a DataStorageServer, which it supervises.
 */
class InstrumentAnalysisServer(val service: String) extends Transactor with PingHandler with Logging {
  
  val actorName = "InstrumentAnalysisServer("+service+")"
  
  /**
   * The message handler calls the "pingHandler" first. If it doesn't match on the
   * message (because it is a PartialFunction), then its own "defaultHandler" is tried.
   */
  def receive = pingHandler orElse defaultHandler 

  def defaultHandler: PartialFunction[Any, Unit] = {

//    case CalculateStatistics(criteria) => self.reply(helper.calculateStatistics(criteria))
    case CalculateStatistics(criteria) => criteria match {
      case CriteriaMap(instruments, statistics, start, end) => 
        log.info("a")
        val results = dataStore.range(start.getMillis, end.getMillis) toList match {
          case Nil => toJValue(Nil)
          case jsons => jsons reduceLeft (_ ++ _) json
        }
        val fullResults = toJValue(Map("criteria" -> toNiceFormat(criteria), "results" -> results))
        log.info("b: "+fullResults)
        self.reply(fullResults)
      case _ =>
        self.reply(toJValue(Map("error" -> ("Invalid criteria: " + criteria))))
    }
        
    case message => 
      log.debug(actorName + ": unexpected message: " + message)
      self.reply(toJValue(Map("error" -> ("Unexpected message: "+message.toString+". Did you forgot to wrap it in a CalculateStatistics object?"))))
  }

  lazy val dataStore = makeDefaultDataStore(service+"_data_store")
  
  protected def makeDefaultDataStore(storeName: String): DataStore[JSONRecord] = {
      new InMemoryDataStore[JSONRecord](storeName)
      // new MongoDBDataStore(storeName)
    // val db = System.getProperty("app.datastore.type", config.getString("app.datastore.type", "mongodb"))
    // if (db.toLowerCase.trim == "mongodb") {
    //   log.info("Using MongoDB-backed data storage.")
    //   new MongoDBDataStore(storeName)
    // } else {
    //   log.info("Using in-memory data storage.")
    //   new InMemoryDataStore[JSONRecord](storeName)
    // }
  }
  
  // Extract and format the data so it's more convenient for the UI
  protected def toNiceFormat(criteria: CriteriaMap): Map[String, Any] = 
    Map(
      "instruments" -> Instrument.toSymbolNames(criteria.instruments),
      "statistics"  -> criteria.statistics,
      "start"       -> criteria.start.getMillis,
      "end"         -> criteria.end.getMillis
    )
  
  override protected def subordinatesToPing: List[ActorRef] = Nil
  
//  val helper = new InstrumentAnalysisServerHelper(dataStorageServer)
}

/**
 * A separate helper so we can decouple (most of) the actor-specific code and the logic it performs.
 * TODO: Handle instruments and statistics criteria.
 @ param dataStorageServer by-name parameter to make it lazy!
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
   * becuase clients calling this actor need a synchronous response.
   * TODO: Handle instruments and statistics criteria.
   */
  protected def fetchPrices(
        instruments: List[Instrument], statistics: List[InstrumentStatistic], 
        start: DateTime, end: DateTime): JValue = {
    log.info("1")
    (dataStorageServer ! Get(("start" -> start.getMillis) ~ ("end" -> end.getMillis)))
    Pair("error", instruments.toString)
  }

  protected def fetchPrices2(
        instruments: List[Instrument], statistics: List[InstrumentStatistic], 
        start: DateTime, end: DateTime): JValue = {
    log.info("""dataStorageServer !!! Get(("start" -> start.getMillis) ~ ("end" -> end.getMillis)))""")
    (dataStorageServer !!! Get(("start" -> start.getMillis) ~ ("end" -> end.getMillis))).await.result match {
      case None => log.info("got None!"); Pair("warning", "Nothing returned for query (start, end) = (" + start + ", " + end + ")")
      case Some(result) => log.info("got result: "+result); filter(instruments, statistics, result)
    }
  }
  
  // TODO: Handle instruments and statistics criteria.
  protected def filter(instruments: List[Instrument], statistics: List[InstrumentStatistic], json: JValue): JValue = json

}
