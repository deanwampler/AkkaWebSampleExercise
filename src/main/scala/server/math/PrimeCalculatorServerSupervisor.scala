package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.math._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config.OneForOneStrategy
import se.scalablesolutions.akka.util.Logging

class PrimeCalculatorServerSupervisor extends Actor with ActorSupervision with PingHandler with Logging {
  
  val ONE_HUNDRED_THOUSAND = 100000
  val MILLION = 10 * ONE_HUNDRED_THOUSAND
  val STOP_WAIT_TIMEOUT = 20000
  
  val actorName = "PrimeCalculatorServerSupervisor"

  // TODO: Instead of keeping this state field, restructure the receive method so
  // you can swap in the stop logic for the PrimesCalculationReply case after a 
  // StopCalculatingPrimes is received.
  var stopRequested = false
  
  def handleMessage: PartialFunction[Any, Unit] = {

    case StartCalculatingPrimes => 
      for (i <- 0 until 10) {
        self ! CalculatePrimes(i * ONE_HUNDRED_THOUSAND + 1, (i+1) * ONE_HUNDRED_THOUSAND)
      }
    
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
    case RestartCalculatingPrimes => 
      self !! (StopCalculatingPrimes, STOP_WAIT_TIMEOUT)
      self ! StartCalculatingPrimes

    case CalculatePrimes(from, to) => 
      val i = (from % MILLION) / ONE_HUNDRED_THOUSAND
      log.ifInfo("Sending message to calculate primes for range "+ from + " to " + to)
      val calc = ActorSupervision.getOrMakeActorFor("PrimeCalculator_" + i, Some(self)) {
        name => actorOf(new PrimeCalculatorServer(name))
      }
      calc forward CalculatePrimes(from, to)
    
    case PrimesCalculationReply(from, to, json) =>
      // TODO: What if a calculation never returned? Save the ranges that
      // have been successfully calculated. Invoke the next available range
      // that is still "open".
      if (stopRequested) {
        log.ifInfo("PrimeCalculatorServerSupervisor: Stopping.")
      } else if (to < java.lang.Long.MAX_VALUE - MILLION) {
        val from2 = from + MILLION
        val to2   = to + MILLION
        self ! CalculatePrimes(from2, to2)
      } else {
        log.ifInfo("Stopping to avoid LONG overflow.")
      }
  } 
  
  def receive = handleMessage orElse handleManagementMessage orElse pingHandler
}

object PrimeCalculatorServerSupervisor extends Logging {
  
  def getAllPrimeCalculatorServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[PrimeCalculatorServer]) 
}
  
