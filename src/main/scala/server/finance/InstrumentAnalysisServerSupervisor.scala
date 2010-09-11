package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.util.json._
import org.chicagoscala.awse.persistence._
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
 * Also, this class is where we embed knowledge about how the data on instruments and statistics is 
 * structured and how processing of that data is distributed among InstrumentAnalysisServer.
 */
class InstrumentAnalysisServerSupervisor extends Actor 
    with ActorSupervision with ActorUtil with PingHandler with Logging {
  
  val actorName = "InstrumentAnalysisServerSupervisor"

  /**
   * The message handler calls "pingHandler" first. If it doesn't match on the message
   * (because it is a PartialFunction), then the "defaultHandler" is tried, and finally
   * "unrecognizedMessageHandler" (from the ActorUtil trait) is tried.
   */
  def receive = pingHandler orElse defaultHandler orElse unrecognizedMessageHandler

  def defaultHandler: PartialFunction[Any,Unit] = {
    case CalculateStatistics(criteria) => self.reply(calculate(criteria))
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
  
  /**
   * Where we embed the knowledge described in the class comments above:
   *   1) The instrument resides in a DataStore whose name is given by the first letter of its trading symbol 
   *      (e.g., "A" for "APPL") and the "class" of statistic being calculated. For example, "A_prices".
   *   2) The actual format of timestamps in the JSON: i) The timestamp key is actually "date" and 2) the
   *      format is actually a date string. (This is actually done in the companion object)
   */   
  def getOrMakeInstrumentAnalysisServerFor(instrument: Instrument, statistic: InstrumentStatistic): Some[ActorRef] = {
    val newActorName  = instrument.toString+":"+statistic.toString
    val dataStoreName = instrument.toString.charAt(0).toUpper + "_" + (statistic match {
      case d: Dividend => "dividends"
      case _ => "prices"
    })
    Some(getOrMakeActorFor(newActorName) {
      name => new InstrumentAnalysisServer(name, dataStoreName)
    })
  }  
}

object InstrumentAnalysisServerSupervisor {
  /**
   * Some global initializations:
   *   1) The key in the Mongo records for the timestamp is "date".
   *   2) The timestamp values are actually date strings, not longs.
   * @see InstrumentAnalysisServerSupervisor.getOrMakeInstrumentAnalysisServerFor
   */
  def init = {
    JSONRecord.timestampKey = "date"

    // JSONRecord.timestampConverter = new JSONRecord.TimestampConverter[String] {
    //   // The date (timestamp) values are actually strings, e.g., "2010-08-30".
    //   val persistenceDateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
    //   def millisecondsToTimestamp(millis: Long): String = persistenceDateTimeFormat.print(new DateTime(millis))
    //   def timestampToMilliseconds(ts: String): Long = new DateTime(ts).getMillis
    //   def jValueTimestampToMilliseconds(jvts: JValue): Long = jvts match {
    //     case JString(s) => timestampToMilliseconds(s)
    //     case _ => throw new JSONRecord.InvalidJSONException(jvts)
    //   }
    // }
  }
}

