package org.chicagoscala.awse.server.math
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config.OneForOneStrategy
import se.scalablesolutions.akka.util.Logging

sealed trait PrimeCalculationMessages
object PrimeCalculationMessages {
  val DEFAULT_RANGE_MIN = 1L
  val DEFAULT_RANGE_MAX = 1000000L  
}
case class StartCalculatingPrimes(
  from: Long = PrimeCalculationMessages.DEFAULT_RANGE_MIN, 
  to: Long   = PrimeCalculationMessages.DEFAULT_RANGE_MAX) extends PrimeCalculationMessages
case class RestartCalculatingPrimes(
  from: Long = PrimeCalculationMessages.DEFAULT_RANGE_MIN, 
  to: Long   = PrimeCalculationMessages.DEFAULT_RANGE_MAX) extends PrimeCalculationMessages
case object StopCalculatingPrimes extends PrimeCalculationMessages
case class  PrimesCalculationReply(from: Long, to: Long, primesJSON: String) extends PrimeCalculationMessages

class PrimeCalculatorServerSupervisor extends Actor with ActorSupervision with PingHandler with Logging {
  import PrimeCalculatorServerSupervisor._
  
  val actorName = "PrimeCalculatorServerSupervisor"

  // TODO: Instead of keeping this state field, restructure the receive method so
  // you can swap in the stop logic for the PrimesCalculationReply case after a 
  // StopCalculatingPrimes is received.
  var stopRequested = false
  
  def handleMessage: PartialFunction[Any, Unit] = {

    case StartCalculatingPrimes(min, max) => doStartCalculatingPrimes(min, max)
    
    // See PrimeCalculatorServer for how it "participates" in stopping.
    // TODO: There is probably lots of work can be done to make this process more robust 
    // and to really clean out old actors, data, etc. Would it just be better to restart
    // Jetty? Why or why not?
    case StopCalculatingPrimes => 
      // TODO: Is the following logic safe?
      if (! stopRequested) {
        stopRequested = true
        PrimeCalculatorServerSupervisor.getAllPrimeCalculatorServers foreach { _ ! Stop }
      }
      
    // TODO: Make it truly wait for all old actors to finish. (Hint: Use Akka's ActorRegistry?)
    // What else, if anything, needs to be done to cleanly remove all old actors and data, so we start fresh?
    case RestartCalculatingPrimes(min, max) => 
      self !! (StopCalculatingPrimes, STOP_WAIT_TIMEOUT)
      doStartCalculatingPrimes(min, max)

    // TODO: delete
    // case PrimesCalculationReply(from, to, json) =>
    //   // TODO: What if a calculation never returned? Save the ranges that
    //   // have been successfully calculated. Invoke the next available range
    //   // that is still "open".
    //   if (stopRequested) {
    //     log.ifInfo("PrimeCalculatorServerSupervisor: Stopping.")
    //   } else if (to < java.lang.Long.MAX_VALUE - MILLION) {
    //     val from2 = from + MILLION
    //     val to2   = to + MILLION
    //     self ! CalculatePrimes(from2, to2)
    //   } else {
    //     log.ifInfo("Stopping to avoid LONG overflow.")
    //   }
  } 
  
  def receive = handleMessage orElse handleManagementMessage orElse pingHandler

  def doStartCalculatingPrimes(min: Long, max: Long) = for {
    i <- min until max by ONE_HUNDRED_THOUSAND
    j = if (i+ONE_HUNDRED_THOUSAND < max) (i+ONE_HUNDRED_THOUSAND) else max
  } {
    val id = (i % MILLION) / ONE_HUNDRED_THOUSAND
    log.ifInfo("Sending message to calculate primes for range "+ i + " to " + j)
    val calc = ActorSupervision.getOrMakeActorFor("PrimeCalculator_" + id, Some(self)) {
      name => actorOf(new PrimeCalculatorServer(name))
    }
    calc ! StartCalculatingPrimes(i, j)
  }
}

object PrimeCalculatorServerSupervisor extends Logging {
  
  val ONE_HUNDRED_THOUSAND = 100000
  val MILLION = 10 * ONE_HUNDRED_THOUSAND
  val STOP_WAIT_TIMEOUT = 20000
  
  def getAllPrimeCalculatorServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[PrimeCalculatorServer]) 
}
  
