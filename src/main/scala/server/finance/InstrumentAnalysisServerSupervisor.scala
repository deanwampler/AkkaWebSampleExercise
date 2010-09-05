package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.util.json._
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
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

/**
 * Supervisor for InstrumentAnalysisServers. It also has methods that are called directly to
 * invoke InstrumentAnalysisServers to do calculations, return data, etc.
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
  
  def getOrMakeInstrumentAnalysisServerFor(instrument: Instrument, statistic: InstrumentStatistic): Some[ActorRef] = {
    val newActorName = instrument.toString+":"+statistic.toString
    Some(getOrMakeActorFor(newActorName) {
      name => new InstrumentAnalysisServer(name)
    })
  }  
}
