package org.chicagoscala.awse.server.math
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.math._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging
import org.joda.time._

class DataStorageNotAvailable(service: String) extends RuntimeException(
  "Could not get a DataStorageServer for prime calculator actor "+service)
  
/**
 * PrimeCalculatorServer is a worker that calculates prime numbers for a given range.
 * It sends the results to a DataStorageServer, which it supervises.
 */
class PrimeCalculatorServer(val service: String) extends Actor with ActorSupervision with PingHandler with Logging {
  
  val actorName = "PrimeCalculatorServer("+service+")"
  
  def receive = defaultHandler orElse pingHandler

  def defaultHandler: PartialFunction[Any, Unit] = {

    case StartCalculatingPrimes(min: Long, max: Long) => calcPrimes(min: Long, max: Long)
      
    case StopCalculatingPrimes => handleStop
    
    case message => log.ifTrace(actorName + ": ignoring " + message)
    
    // TODO: Beta1 compiler bug!!! Uncomment this line and the compiler crashes.
    // case Pair(String, _) => // message response
  }

  override protected def afterPing(ping: Pair[String,String]) = (dataStore !! ping) match {
    case Some(result) => result
    case None => "No reply min datastore " + dataStore
  }

  protected def toJSON(primes: List[Long]) = primes.size match {
    case 0 => "[]"
    case n => "[" + primes.map(_.toString).reduceLeft(_ + ", " + _) + "]"
  }
  
  protected def prefix(min: Long, max: Long, size: Long) =
    """{"min": """ + min + """, "max": """ + max + """, "number-of-primes": """ + size
    
    
  protected lazy val dataStore: ActorRef = {
    val ds = actorOf(new DataStorageServer(service+"_DataStoreServer"))
    self link ds
    ds.start
    ds
  }
  
  protected def calcPrimes(min: Long, max: Long) {
    val primes = Primes(min, max)
    val json = prefix(min, max, primes.size) + """, "primes": """ + toJSON(primes)  + "}"
    log.info(actorName+": Calculated "+primes.size+" primes between "+min+" and "+max)
    log.ifDebug("Sending data to the DataStorageServer...")
    dataStore ! Put(json)
    self.reply (PrimesCalculationReply(min, max, prefix(min, max, primes.size) + "}"))
  }

  // TODO: We notify the data server to stop. Would it be better to go through 
  // it's supervisor in some way?
  protected def handleStop = {
    log.info("Sending stop to the DataStorageServer...")
    dataStore ! Stop
    self unlink dataStore
    self stop
  }
}
