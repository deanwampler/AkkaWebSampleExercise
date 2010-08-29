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

case class CalculateStatistics(criteria: Map[String,Any]) extends InstrumentCalculationMessages
    
case object StopCalculating extends InstrumentCalculationMessages

class InstrumentAnalysisServerSupervisor extends Actor with ActorSupervision with PingHandler with Logging {
  import InstrumentAnalysisServerSupervisor._
  
  val actorName = "InstrumentAnalysisServerSupervisor"

  // TODO: Instead of keeping this state field, swap a different PartialFunction for handleMessage
  var stopRequested = false
  
  def handleMessage: PartialFunction[Any, Unit] = {

    case CalculateStatistics(criteria) => doStartStatistics(criteria)
      
    // See InstrumentAnalysisServer for how it "participates" in stopping.
    // TODO: There is probably lots of work can be done to make this process more robust 
    // and to really clean out old actors, data, etc. Would it just be better to restart
    // Jetty? Why or why not?
    case StopCalculating => 
      // TODO: Is the following logic safe?
      if (! stopRequested) {
        stopRequested = true
        InstrumentAnalysisServerSupervisor.getAllInstrumentAnalysisServers foreach { _ !! Stop }
      }
  } 
  
  def receive = handleMessage orElse handleManagementMessage orElse pingHandler

  // TODO: Uses one actor for each instrument, but is that more efficient than using one actor for N > 1?
  def doStartStatistics (criteria: Map[String,Any]) = 
    for {
      instrument <- criteria.get("instruments")
      calculator = ActorSupervision.getOrMakeActorFor("MovingAverageCalculator_" + instrument, Some(self)) {
        name => actorOf(new InstrumentAnalysisServer(name))
      }
      statistic  <- criteria.get("statistics")
    } calculator ! CalculateStatistics(criteria ++ Map("instrument" -> instrument, "statistic" -> statistic))

}

object InstrumentAnalysisServerSupervisor extends Logging {
  
  def getAllInstrumentAnalysisServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[InstrumentAnalysisServer]) 
}
  
