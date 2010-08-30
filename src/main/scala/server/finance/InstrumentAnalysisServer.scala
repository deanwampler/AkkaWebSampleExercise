package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.domain.finance.FinanceJSONConverter._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

class DataStorageNotAvailable(service: String) extends RuntimeException(
  "Could not get a DataStorageServer for "+service)
  
/**
 * InstrumentAnalysisServer is a worker that calculates (or simply fetches...) statistics for financial instruments.
 * It reads data from and writes results to a DataStorageServer, which it supervises.
 */
class InstrumentAnalysisServer(val service: String) extends Actor with ActorSupervision with PingHandler with Logging {
  
  val actorName = "InstrumentAnalysisServer("+service+")"
  
  def receive = defaultHandler orElse pingHandler

  def defaultHandler: PartialFunction[Any, Unit] = {

    case CalculateStatistics(criteria) => self.reply(helper.calculateStatistics(criteria))
    
    case message => log.trace(actorName + ": unexpected message: " + message)
  }

  override protected def afterPing(ping: Pair[String,String]) = dataStorageServers map { ds =>
    (ds !! ping) match {
      case Some(result) => result
      case None => "No response from " + dataStorageServer
    }
  } mkString(", ")
  
  protected lazy val dataStorageServer: ActorRef = makeDataStorage(service)
  protected var dataStorageServers: List[ActorRef] = Nil
  
  protected def makeDataStorage(name: String) = {
    val ds = actorOf(new DataStorageServer(name+"_DataStorageServer"))
    dataStorageServers ::= ds
    self link ds
    ds.start
    ds
  }
  
  protected def handleStop = {
    log.info("Sending stop to the DataStorageServers...")
    dataStorageServers foreach { ds =>
      ds ! Stop 
      self unlink ds
    }
    self stop
  }

  val helper = new InstrumentAnalysisServerHelper(dataStorageServer)
}

/**
 * A separate helper so we can decouple (most of) the actor-specific code and the logic it performs.
 * TODO: Handle instruments and statistics criteria
 */
class InstrumentAnalysisServerHelper(dataStorageServer: ActorRef) {
  
  def calculateStatistics(criteria: CriteriaMap) = criteria match {
    case CriteriaMap(instruments, statistics, start, end) => 
      fetchPrices(start, end)
    case _ =>
      """{"error": "Invalid criteria: """ + criteria + "\"}"
  }

  // Must make a synchronous call to the data store server
  protected def fetchPrices(start: DateTime, end: DateTime) = {
    (dataStorageServer !!! Get(("start" -> start.getMillis) ~ ("end" -> end.getMillis))).await.result match {
      case None => """{"warning": "Nothing returned for query (start, end) = (""" + start + ", " + end + ")\"}"
      case Some(x) => x
    }
  }
}
