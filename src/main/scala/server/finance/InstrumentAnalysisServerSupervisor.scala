package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.util.json._
import org.chicagoscala.awse.util.Logging
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence.mongodb._
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.domain.finance._
import akka.actor._
import akka.actor.Actor._
import akka.dispatch.Futures
import org.joda.time._
import org.joda.time.format._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

/**
 * Supervisor for InstrumentAnalysisServers. It also has methods that are called
 * directly to invoke InstrumentAnalysisServers to do calculations, return data, etc.
 * Also, this class and its companion are where we embed knowledge about how the data 
 * on instruments and statistics is structured and how processing of that data is 
 * distributed among InstrumentAnalysisServer, etc.
 */
class InstrumentAnalysisServerSupervisor extends Actor with ActorFactory 
    with ActorUtil with PingHandler with Logging {
  
  def actorName = "InstrumentAnalysisServerSupervisor"

  /**
   * The message handler calls "pingHandler" first. If it doesn't match on the message
   * (because it is a PartialFunction), then the "defaultHandler" is tried, and finally
   * "unrecognizedMessageHandler" (from the ActorUtil trait) is tried.
   */
  def receive = pingHandler orElse defaultHandler orElse unrecognizedMessageHandler

  def defaultHandler: PartialFunction[Any,Unit] = {
    case CalculateStatistics(criteria) => 
      self.reply(calculate(criteria))
    case GetInstrumentList(symbolFirstLetters, keyForInstrumentSymbols) => 
      self.reply(getInstrumentList(symbolFirstLetters, keyForInstrumentSymbols))
  }
  
  /**
   * Ping the InstrumentAnalysisServers (if any currently exist) and return their responses.
   */
  override protected def subordinatesToPing: List[ActorRef] =
    getAllInstrumentAnalysisServers
    
  def getAllInstrumentAnalysisServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[InstrumentAnalysisServer]).toList

  def calculate (criteria: CriteriaMap) = {
    val futures = for {
      instrument <- criteria.instruments
      statistic  <- criteria.statistics
      calculator <- getOrMakeInstrumentAnalysisServerFor(instrument)
    } yield (calculator !!! CalculateStatistics(criteria.withInstruments(instrument).withStatistics(statistic)))
    Futures.awaitAll(futures)
    futuresToJSON(futures, "None!")
  }
  
  def getInstrumentList(symbolList: List[Char], keyForInstrumentSymbols: String) = {
    val futures = for {
      letter <- symbolList
      instrument = Instrument(letter.toString)
      calculator <- getOrMakeInstrumentAnalysisServerFor(instrument)
    } yield (calculator !!! GetInstrumentList(List(letter), keyForInstrumentSymbols))
    Futures.awaitAll(futures.toList)
    futuresToJSON(futures.toList, "None!")
  }
  
  protected def collectionNameFromSymbol(symbol: String, suffix: String) = 
    symbol.charAt(0).toUpper + "_" + suffix
  
  /**
   * Where we embed some of the knowledge described in the class comments above. Specifically, the
   * instrument resides in a DataStore whose name is given by the first letter of its trading symbol 
   * (e.g., "A" for "APPL") and the "class" of statistic being calculated. For example, "A_prices".
   * Also, the timestamp values are date strings. (Handled by JSONRecord automatically)
   */   
  def getOrMakeInstrumentAnalysisServerFor(instrument: Instrument): Some[ActorRef] = {
    Some(getOrMakeActorFor(instrument.symbol) {
      name => new InstrumentAnalysisServer(name, 
        InstrumentAnalysisServerSupervisor.dataStorageServerFactory(collectionNameFromSymbol(instrument.symbol,"prices")),
        InstrumentAnalysisServerSupervisor.dataStorageServerFactory(collectionNameFromSymbol(instrument.symbol,"dividends")))
    })
  }
}

/**
 * Encodes some global features:
 *   1) The key in the Mongo records for the timestamp is "date".
 *   2) The values for the timestamp are strings of form "yyyy-MM-dd".
 * @see InstrumentAnalysisServerSupervisor.getOrMakeInstrumentAnalysisServerFor
 */
object InstrumentAnalysisServerSupervisor {

  def init = {
    JSONRecord.timestampKey = "date"
  }
  
  def dataStorageServerFactory(storeName: String): ActorRef = {
    import akka.config.Config.config

    val dataStoreKind = System.getProperty("app.datastore.type", config.getString("app.datastore.type", "mongodb"))
    val dataStore = if (dataStoreKind.toLowerCase.trim == "mongodb") {
      log.info("Using MongoDB-backed data storage. name = "+storeName)
      // We actually store DateTimes as Date strings.
      new MongoDBDataStore(storeName)(dateTime => YearMonthDayTimestamp(dateTime).toString) 
    } else {
      log.info("Using in-memory data storage. name = "+storeName)
      new InMemoryDataStore(storeName) // always uses DateTime for queries
    }
    val ref = actorOf(new DataStorageServer(storeName, dataStore))
    ref.id = storeName
    ref
  }
}
