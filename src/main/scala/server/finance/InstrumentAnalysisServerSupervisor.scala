package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.util.json._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence.mongodb._
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.domain.finance._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.dispatch.Futures
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config.OneForOneStrategy
import se.scalablesolutions.akka.util.Logging
import org.joda.time._
import org.joda.time.format._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

/**
 * Supervisor for InstrumentAnalysisServers. It also has methods that are called directly to
 * invoke InstrumentAnalysisServers to do calculations, return data, etc.
 * Also, this class and its companion are where we embed knowledge about how the data 
 * on instruments and statistics is structured and how processing of that data is 
 * distributed among InstrumentAnalysisServer, etc.
 */
class InstrumentAnalysisServerSupervisor extends Actor with ActorFactory with ActorUtil with PingHandler with Logging {
  
  val actorName = "InstrumentAnalysisServerSupervisor"

  /**
   * The message handler calls "pingHandler" first. If it doesn't match on the message
   * (because it is a PartialFunction), then the "defaultHandler" is tried, and finally
   * "unrecognizedMessageHandler" (from the ActorUtil trait) is tried.
   */
  def receive = pingHandler orElse defaultHandler orElse unrecognizedMessageHandler

  def defaultHandler: PartialFunction[Any,Unit] = {
    case CalculateStatistics(criteria) => self.reply(calculate(criteria))
    case GetInstrumentList(range, keyForInstrumentSymbols) => self.reply(getInstrumentList(range, keyForInstrumentSymbols))
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
      calculator <- getOrMakeInstrumentAnalysisServerFor(instrument, statistic)
    } yield (calculator !!! CalculateStatistics(criteria.withInstruments(instrument).withStatistics(statistic)))
    Futures.awaitAll(futures)
    futuresToJSON(futures, "None!")
  }
  
  def getInstrumentList(range: scala.collection.immutable.NumericRange[Char], keyForInstrumentSymbols: String) = {
    val futures = for {
      letter <- range
      oneLetterRange = letter to letter
      instrument = Instrument(letter.toString)
      calculator <- getOrMakeInstrumentAnalysisServerFor(instrument, Price(Dollars))
    } yield (calculator !!! GetInstrumentList(oneLetterRange, keyForInstrumentSymbols))
    Futures.awaitAll(futures.toList)
    futuresToJSON(futures.toList, "None!")
  }
  
  /**
   * Where we embed some of the knowledge described in the class comments above. Specifically, the
   * instrument resides in a DataStore whose name is given by the first letter of its trading symbol 
   * (e.g., "A" for "APPL") and the "class" of statistic being calculated. For example, "A_prices".
   * Also, the timestamp values are date strings. (Handled by JSONRecord automatically)
   */   
  def getOrMakeInstrumentAnalysisServerFor(instrument: Instrument, statistic: InstrumentStatistic): Some[ActorRef] = {
    val newActorName  = instrument.symbol+":"+statistic.toString
    val dataStoreName = instrument.symbol.charAt(0).toUpper + "_" + (statistic match {
      case d: Dividend => "dividends"
      case _ => "prices"
    })
    Some(getOrMakeActorFor(newActorName) {
      name => new InstrumentAnalysisServer(
        name, InstrumentAnalysisServerSupervisor.dataStorageServerFactory(dataStoreName))
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
    import se.scalablesolutions.akka.config.Config.config

    val dataStoreKind = System.getProperty("app.datastore.type", config.getString("app.datastore.type", "mongodb"))
    val dataStore = if (dataStoreKind.toLowerCase.trim == "mongodb") {
      log.info("Using MongoDB-backed data storage. name = "+storeName)
      new MongoDBDataStore(storeName) {
        // We actually store DateTimes as Date strings.
        val format = DateTimeFormat.forPattern("yyyy-MM-dd")
        
        override protected def dateTimeToAnyValue(dateTime: DateTime): Any = format.print(dateTime)
      }
    } else {
      log.info("Using in-memory data storage. name = "+storeName)
      new InMemoryDataStore(storeName) // always uses DateTime for queries
    }
    val id = storeName+"_data_storage_server"
    val ref = actorOf(new DataStorageServer(id, dataStore))
    ref.id = id
    ref
  }
}
