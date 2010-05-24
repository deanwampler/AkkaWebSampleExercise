package org.chicagoscala.awse.server.rest
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.server.math._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.dispatch.{Future, Futures}
import se.scalablesolutions.akka.util.Logging
import net.lag.logging.Level                                   
import javax.ws.rs._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import org.joda.time._

@Path("/")
class RestfulDataServer extends Actor with NamedActor with Logging {
   
  val actorName = "RestfulDataServer"
  
  def dataStorageServers = DataStorageServerSupervisor.getAllDataStorageServers
  
  def receive = {
    case message => 
      try {
        log.ifInfo("RestfulDataServer: message: "+message)
      } catch {
        case ex => log.error("RestfulDataServer: exception: "+ex)
        throw ex
      }
  }

  /**
   * Get all data for the requested "action", where only one action is supported:
   *   primes: Return the primes that have been calculated thus far, subject to
   *           the following URL options: ?min=long&max=long
   * where <tt>min</tt> is the earliest time for data to return, inclusive (defaults to 0 - 1/1/1970), 
   * and <tt>max</tt>  defaults to the most recent timestamp for data to return, exclusive (defaults
   * to return the most recent data). Both timestamps are in milliseconds.
   */
  @GET
  @Path("{action}")
  @Produces(Array("application/json"))
  def getAllDataFor(
      @PathParam("action") action: String, 
      @DefaultValue("0")  @QueryParam("min") fromTime: Long,
      @DefaultValue("-1") @QueryParam("max") untilTime: Long ): String = 
    action match {
      case "ping" => 
        log.info("Pinging!!")
        val futures = List(
          DataStorageServerSupervisor.instance !!! Pair("ping", "You there??"),
          PrimeCalculatorServerSupervisor.instance !!! Pair("ping", "You there??"))
        Futures.awaitAll(futures)
        val replyMessage = """{"ping replies": """ + handlePingReplies(futures) + "}"
        log.info("Ping replies: "+replyMessage)
        replyMessage
        
      case "start" => 
        log.info("Starting!!")
        PrimeCalculatorServerSupervisor.instance ! StartCalculatingPrimes
        """{"message": "Started calculating primes."}"""
      
      case "stop" => 
        log.info("Stopping.")
        PrimeCalculatorServerSupervisor.instance ! StopCalculatingPrimes
        """{"message": "Started calculating primes."}"""

      case "primes" =>
        val from  = new DateTime(fromTime)
        val until = if (untilTime > 0) new DateTime(untilTime) else new DateTime
        val dsServers = dataStorageServers
        if (dsServers.size == 0) {
          val message = "RestfulDataServer: No DataStorageServers! (normal at startup)"
          log.warning(message)
          "{\"warn\": \"" + message + "\"}"
        } else {
          val futures = dataStorageServers map { server =>
            server !!! Get(from, until)  // fire messages to all data servers...
          }
          try {
            Futures.awaitAll(futures)        // ... and wait for all of them to reply.
            val messageForNone = "No data available for start-end times = "+fromTime+", "+untilTime
            val jsons = for {
              future <- futures
            } yield futureToJSON(future, messageForNone)
            toJSON(jsons)
          } catch {
            case fte:se.scalablesolutions.akka.dispatch.FutureTimeoutException =>
              """{"error": "Actors timed out: """ + fte.getMessage + "\"}"
          }
        }
        
      case x => """{"error": "Unrecognized 'action': """ + action + "\"}"
    }
    
  protected def futureToJSON(future: Future, messageForNone: String) = future.result match {
    case Some(result) => result match {
      case json: String => json
      case _ => """{"error": "Expected JSON data, but got this: """+result+"\"}"
    }
    case None => "{\"error\": \"" + messageForNone + "\"}"
  }

  protected def toJSON(jsons: Iterable[String]) = jsons.size match {
    case 0 => "{\"error\": \"No data servers appear to be available.\"}"
    case _ => "[" + jsons.reduceLeft(_ + ", " + _) + "]"
  }
  
  protected def handlePingReplies(futures: Iterable[Future]) = {
    val messageForNone = "failed!"
    val replies = for { future <- futures } yield futureToJSON(future, messageForNone)
    replies.size match {
      case 0 => "[]"
      case _ => "[" + (replies reduceLeft (_ + ", " + _)) + "]"
    }
  }  
}
