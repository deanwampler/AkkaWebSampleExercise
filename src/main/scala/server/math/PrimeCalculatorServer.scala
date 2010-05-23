package org.chicagoscala.awse.server.math
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.math._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.util.Logging
import org.joda.time._

sealed trait PrimeCalculationMessages
case object StartCalculatingPrimes   extends PrimeCalculationMessages
case object StopCalculatingPrimes    extends PrimeCalculationMessages
case object RestartCalculatingPrimes extends PrimeCalculationMessages
case class  CalculatePrimes(from: Long, to: Long) extends PrimeCalculationMessages
case class  PrimesCalculationReply(from: Long, to: Long, primesJSON: String) extends PrimeCalculationMessages

class DataStorageNotAvailable(service: String) extends RuntimeException(
  "Could not get a DataStorageServer for prime calculator actor "+service)
  
/**
 * PrimeCalculatorServer is a worker that calculates prime numbers for a given range.
 * It sends the results to DataStorageServerSupervisor.
 */
class PrimeCalculatorServer(val service: String) extends Actor with NamedActor with Logging {
  
  val actorName = "PrimeCalculatorServer("+service+")"
  
  def receive = {
    case CalculatePrimes(from: Long, to: Long) => calcPrimes(from: Long, to: Long)
      
    case StopCalculatingPrimes => handleStop
    
    case message => log.ifTrace(actorName + ": ignoring " + message)
    
    // TODO: Beta1 compiler bug!!! Uncomment this line and the compiler crashes.
    // case Pair(String, _) => // message response
  }

  protected def toJSON(primes: List[Long]) = primes.size match {
    case 0 => "[]"
    case n => "[" + primes.map(_.toString).reduceLeft(_ + ", " + _) + "]"
  }
  
  protected def prefix(from: Long, to: Long, size: Long) =
    """{"from": """ + from + """, "to": """ + to + """, "number-of-primes": """ + size
    
    
  protected def dataStore: Option[Actor] =
      Some(DataStorageServerSupervisor.instance.getOrMakeActorFor(service+"_DataStoreServer"))

  // TODO: For some reason, sending a message to the supervisor fails! It may be that it has become
  // unresponsive for some reason. In contrast, calling the getOrMakeActorFor() method, as above,
  // works fine.
  protected def dataStore2: Option[Actor] = { 
    val result: Option[DataStorageServer] = 
      DataStorageServerSupervisor.instance !! GetActorFor(service+"_DataStoreServer") 
    result match {
      case Some(dss) => result
      case None => 
        log.error("Can't get a DataStorageServer for name "+service+"_DataStoreServer!!")
        result
    }
  }
  
  protected def calcPrimes(from: Long, to: Long) {
    val primes = Primes(from, to)
    val json = prefix(from, to, primes.size) + """, "primes": """ + toJSON(primes)  + "\"}"
    log.info(actorName+": Calculated "+primes.size+" primes between "+from+" and "+to)
    dataStore match {
      case Some(dss) => 
        log.info("Sending data to the DataStorageServer...")
        dss ! Put(new DateTime(), json)
        reply (PrimesCalculationReply(from, to, prefix(from, to, primes.size) + "\"}"))
      case None =>
        // TODO Send a reply that you can't store the data, so try again...
    } 
  }

  // TODO: We notify the data server to stop. Would it be better to go through 
  // it's supervisor in some way?
  protected def handleStop = {
    PrimeCalculatorServerSupervisor.instance ! Unregister(this)
    dataStore match {
      case Some(dss) => 
        log.info("Sending stop to the DataStorageServer...")
        dss ! Stop
      case None =>
    } 
    this.stop
  }
  
}
