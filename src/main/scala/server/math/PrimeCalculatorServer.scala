package org.chicagoscala.awse.server.math
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.math._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.util.Logging
import org.joda.time._

sealed trait PrimeCalculationMessages
case object StartCalculatingPrimes extends PrimeCalculationMessages
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
    case CalculatePrimes(from: Long, to: Long) => 
      val primes = Primes(from, to)
      val json = prefix(from, to, primes.size) + ", " + toJSON(primes)  + "\"}"
      log.info(actorName+": Calculated "+primes.size+" primes between "+from+" and "+to)
      dataStore match {
        case Some(dss) => 
          log.info("Sending data to the DataStorageServer...")
          dss ! Put(new DateTime(), json)
        case None =>
      } 
      reply (PrimesCalculationReply(from, to, prefix(from, to, primes.size) + "\"}"))
      
    case msg => log.ifTrace(actorName + ": ignoring " + msg)
    
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
      Some(DataStorageServerSupervisor.dataStorageServerSupervisor.getOrMakeActorFor(service+"_DataStoreServer"))

  //
  protected def dataStore2: Option[Actor] = {
    val result: Option[DataStorageServer] = 
      DataStorageServerSupervisor.dataStorageServerSupervisor !! GetActorFor(service+"_DataStoreServer") 
    result match {
      case Some(dss) => result
      case None => 
        log.error("Can't get a DataStorageServer for name "+service+"_DataStoreServer!!")
        result
    }
  }
}
