package org.chicagoscala.awse.server
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.server.math._
import org.chicagoscala.awse.server.rest._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config.OneForOneStrategy
import se.scalablesolutions.akka.util.Logging

class BootAWSESupervisor {
  val factory = SupervisorFactory(
    SupervisorConfig(
      RestartStrategy(OneForOne, 5, 5000, List(classOf[Throwable])),
      Supervise(
        DataStorageServerSupervisor.dataStorageServerSupervisor,
        LifeCycle(Permanent)) :: 
      Supervise(
        PrimeCalculatorServerSupervisor.primeCalculatorServerSupervisor,
        LifeCycle(Permanent)) :: 
      Supervise(
        new RestfulDataServer,
        LifeCycle(Permanent)) :: 
      Nil))
      
  DataStorageServerSupervisor.dataStorageServerSupervisor.start
  PrimeCalculatorServerSupervisor.primeCalculatorServerSupervisor.start
  factory.newInstance.start
}

class DataStorageServerSupervisor extends Actor with ActorSupervision {
  trapExit = List(classOf[Throwable])
  faultHandler = Some(OneForOneStrategy(5, 5000))
  lifeCycle = Some(LifeCycle(Permanent))
  
  protected def makeActor(actorName: String): Actor = new DataStorageServer(actorName)
  
  def receive = handleManagementMessage
}

object DataStorageServerSupervisor extends Logging {
    
  lazy val dataStorageServerSupervisor = new DataStorageServerSupervisor
}
  
class PrimeCalculatorServerSupervisor extends Actor with ActorSupervision with Logging {
  trapExit = List(classOf[Throwable])
  faultHandler = Some(OneForOneStrategy(5, 5000))
  lifeCycle = Some(LifeCycle(Permanent))
  
  val ONE_HUNDRED_THOUSAND = 100000
  val MILLION = 10 * ONE_HUNDRED_THOUSAND

  protected def makeActor(actorName: String): Actor = new PrimeCalculatorServer(actorName)

  def handleMessage: PartialFunction[Any, Unit] = {

    case StartCalculatingPrimes => 
      for (i <- 0 until 10) {
        this ! CalculatePrimes(i * ONE_HUNDRED_THOUSAND + 1, (i+1) * ONE_HUNDRED_THOUSAND)
      }
    
    case CalculatePrimes(from, to) => 
      val i = (from % MILLION) / ONE_HUNDRED_THOUSAND
      log.ifInfo("Sending message to calculate primes for range "+ from + " to " + to)
      getOrMakeActorFor("PrimeCalculator_" + i) forward CalculatePrimes(from, to)
    
    case PrimesCalculationReply(from, to, json) =>
      // TODO: What if a calculation never returned? Save the ranges that
      // have been successfully calculated. Invoke the next available range
      // that is still "open".
      if (to < java.lang.Long.MAX_VALUE - MILLION) {
        val from2 = from + MILLION
        val to2   = to + MILLION
        this ! CalculatePrimes(from2, to2)
      } else {
        log.ifInfo("Stopping to avoid LONG overflow.")
      }
  } 
  
  def receive = handleMessage orElse handleManagementMessage
}

object PrimeCalculatorServerSupervisor extends Logging {
    
  lazy val primeCalculatorServerSupervisor = new PrimeCalculatorServerSupervisor
  
  def getAllPrimeCalculatorServers: List[Actor] = 
    ActorRegistry.actorsFor(classOf[PrimeCalculatorServer]) 
}
  
