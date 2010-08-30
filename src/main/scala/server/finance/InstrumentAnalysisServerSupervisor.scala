package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.domain.finance._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config.OneForOneStrategy
import se.scalablesolutions.akka.util.Logging
import org.joda.time._

sealed trait InstrumentCalculationMessages

case class CalculateStatistics(criteria: CriteriaMap) extends InstrumentCalculationMessages
    
/**
 * Supervises InstrumentAnalysisServers. 
 */
class InstrumentAnalysisServerSupervisor extends Actor with ActorSupervision with PingHandler with Logging {
  import InstrumentAnalysisServerSupervisor._
  
  val actorName = "InstrumentAnalysisServerSupervisor"

  val defaultHandler: PartialFunction[Any,Unit] = {
    case CalculateStatistics(criteria) => calculate(criteria)
  }
  
  def receive = defaultHandler orElse handleManagementMessage orElse pingHandler

  /**
   * Calculate the statistics using one actor for each instrument and statistic combination.
   * Used to balance the load of calculating statistics based on the input criteria. 
   * TODO: Would it be more efficient to have each actor handle more than one instrument and/or statistic? Note that
   * InstrumentAnalysisServer does not assume it works on only one instrument and one statistic at a time.
   * Note: If there were many supervisors, this method could ignore criteria out this supervisor's scope, e.g., 
   * instruments handled by another supervisor.
   * @return a list of Futures
   */
  protected def calculate (criteria: CriteriaMap) = 
    for {
      instrument <- criteria.instruments
      statistic  <- criteria.statistics
      calculator = ActorSupervision.getOrMakeActorFor(instrument+":"+statistic, Some(self)) {
        name => actorOf(new InstrumentAnalysisServer(name))
      }
    } yield (calculator !!! criteria.withInstruments(instrument).withStatistics(statistic))

}

object InstrumentAnalysisServerSupervisor extends Logging {
  
  def getAllInstrumentAnalysisServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[InstrumentAnalysisServer]).toList
}
  
