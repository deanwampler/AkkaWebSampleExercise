package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.domain.finance._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging
import org.joda.time._

class DataStorageNotAvailable(service: String) extends RuntimeException(
  "Could not get a DataStorageServer for "+service)
  
/**
 * InstrumentAnalysisServer is a worker that calculates statistics for financial instruments.
 * It reads data from and writes results to a DataStorageServer, which it supervises.
 */
class InstrumentAnalysisServer(val service: String) extends Actor with ActorSupervision with PingHandler with Logging {
  
  val actorName = "InstrumentAnalysisServer("+service+")"
  
  def receive = defaultHandler orElse pingHandler

  def defaultHandler: PartialFunction[Any, Unit] = {

    case CalculateStatistics(criteria) => 
      val startingAt  = criteria.getOrElse("startingAt", new DateTime(0)).asInstanceOf[DateTime]
      val upTo        = criteria.getOrElse("upTo", new DateTime).asInstanceOf[DateTime]
      val instruments = criteria.getOrElse("instruments", List[Instrument]()).asInstanceOf[List[Instrument]]
      val statistics  = criteria.getOrElse("statistics", List[InstrumentStatistic]()).asInstanceOf[List[InstrumentStatistic]]
      for {
        instrument <- instruments
        statistic  <- statistics
      } calcStats(instrument, statistic, startingAt, upTo)
    
    case StopCalculating => handleStop
    
    case message => log.ifTrace(actorName + ": unexpected message: " + message)
  }

  override protected def afterPing(ping: Pair[String,String]) = dataStores map { ds =>
    (ds !! ping) match {
      case Some(result) => result
      case None => "No response from " + dataStore
    }
  } mkString(", ")
  
  protected lazy val dataStore: ActorRef = makeDataStore(service)
  protected var dataStores: List[ActorRef] = Nil
  
  protected def makeDataStore(name: String) = {
    val ds = actorOf(new DataStorageServer(name+"_DataStorageServer"))
    dataStores ::= ds
    self link ds
    ds.start
    ds
  }
  
  protected def calcStats(instrument: Instrument, statistic: InstrumentStatistic, startingAt: DateTime, upTo: DateTime) = 
    statistic match {
      case ma: MovingAverage => calcMovingAverage(instrument, ma, startingAt, upTo)
      case _ => error("Unknown statistic: " + statistic)
    }    
  
  protected def calcMovingAverage(instrument: Instrument, average: MovingAverage, startingAt: DateTime, upTo: DateTime) = {
    // Fetch data for instrument
    // calculate average over instrument.
    // write result to store:
    val averageStore = makeDataStore(average.toString)
  }
  
  protected def handleStop = {
    log.info("Sending stop to the DataStorageServers...")
    dataStores foreach { ds =>
      ds ! Stop 
      self unlink ds
    }
    self stop
  }
}
