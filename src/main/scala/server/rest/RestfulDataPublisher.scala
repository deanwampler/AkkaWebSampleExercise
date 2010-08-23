package org.chicagoscala.awse.server.rest
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.server.math._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.dispatch.{Future, Futures, FutureTimeoutException}
import se.scalablesolutions.akka.util.Logging
import net.lag.logging.Level                                   
import javax.ws.rs._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import org.joda.time._

@Path("/")
class RestfulDataPublisher extends Logging {
   
  val actorName = "RestfulDataPublisher"
  
  def dataStorageServers = ActorRegistry.actorsFor(classOf[DataStorageServer])
  def primeCalculatorServerSupervisors = ActorRegistry.actorsFor(classOf[PrimeCalculatorServerSupervisor])
  def primeCalculatorServers = ActorRegistry.actorsFor(classOf[PrimeCalculatorServer])
  
  /**
   * Handle a rest request. The following "actions" are supported:
   *   start:   Start calculating primes, subject to the URL options: ?min=long&max=long, the minimum and maximum
   *            range to search for values, inclusive. If not specified, they default to 1 to 1M.
   *   stop:    Stop the calculation of primes. The calculation stops automatically once the requested range of
   *            primes have been been calculated.
   *   restart: Stop, then start calculating primes, subject to the URL options: ?min=long&max=long, the minimum and maximum
   *   primes:  Return the all primes that have been calculated thus far or limited to specified range: ?min=long&max=long
   *   ping:    Send a "ping" message to each actor and return their replies.
   * @todo: It would simplify both the web code and this code to use a websocket to stream results to the browser 
   * as they are completed. Consider also Atmosphere 6 and its JQuery plugin as an abstraction that supports
   * websockets, but can degrade to Comet, etc., when used with a browser-server combination that doesn't support
   * websockets (@see http://jfarcand.wordpress.com/2010/06/15/using-atmospheres-jquery-plug-in-to-build-applicationsupporting-both-websocket-and-comet/).
   */
  @GET
  @Path("{action}")
  @Produces(Array("application/json"))
  def restRequest(
      @PathParam("action") action: String, 
      @DefaultValue(PrimeCalculationMessages.DEFAULT_RANGE_MIN) @QueryParam("min") min: Long,
      @DefaultValue(PrimeCalculationMessages.DEFAULT_RANGE_MAX) @QueryParam("max") max: Long ): String = 
    action match {
      case "ping" => 
        try {
          log.info("Pinging!!")
          val futures = (primeCalculatorServerSupervisors ++ primeCalculatorServers ++ dataStorageServers) map {
            _ !!! Pair("ping", "You there??")
          }
          Futures.awaitAll(futures)
          val replyMessage = """{"ping replies": """ + handlePingReplies(futures) + "}"
          log.info("Ping replies: "+replyMessage)
          replyMessage
        } catch {
          case fte: FutureTimeoutException =>
            """{"error": "Actors timed out (""" + fte.getMessage + ").\"}"
        }
        
      case "start" | "stop" | "restart" =>
        val (messageString, message) = determineMessage(action, min, max)
        log.info(messageString)
        ActorRegistry.actorsFor(classOf[PrimeCalculatorServerSupervisor]) foreach { _ ! message }
        """{"message": """ + messageString + "}"
      
      case "primes" =>
        val dsServers = dataStorageServers
        if (dsServers.size == 0) {
          val message = "RestfulDataPublisher: No DataStorageServers! (normal at startup)"
          log.warning(message)
          "{\"warn\": \"" + message + "\"}"
        } else {
          val futures = dataStorageServers map { server =>
            server !!! Get(min, max)  // fire messages to all data servers...
          }
          try {
            Futures.awaitAll(futures)        // ... and wait for all of them to reply.
            val messageForNone = "No data available for start-end times = "+fromTime+", "+untilTime
            val jsons = for {
              future <- futures
            } yield futureToJSON(future, messageForNone)
            toJSON(jsons)
          } catch {
            case fte: FutureTimeoutException =>
              """{"error": "Actors timed out (""" + fte.getMessage + ").\"}"
          }
        }
        
      case x => """{"error": "Unrecognized 'action': """ + action + "\"}"
    }
    
  protected def determineMessage(action: String, min: Long, max: Long) = action match {
    case "start"   => ("Starting calculation",   StartCalculatingPrimes(min, max))
    case "restart" => ("Restarting calculation", RestartCalculatingPrimes(min, max))
    case "stop"    => ("Stopping calculation",   StopCalculatingPrimes)
  }
  
  protected def futureToJSON(future: Future[_], messageForNone: String) = future.result match {
    case Some(result) => result.toString
    case None => "{\"error\": \"" + messageForNone + "\"}"
  }

  protected def toJSON(jsons: Iterable[String]) = jsons.size match {
    case 0 => "{\"error\": \"No data servers appear to be available.\"}"
    case _ => "[" + jsons.reduceLeft(_ + ", " + _) + "]"
  }
  
  protected def handlePingReplies(futures: Iterable[Future[_]]) = {
    val messageForNone = "failed!"
    val replies = for { future <- futures } yield futureToJSON(future, messageForNone)
    replies.size match {
      case 0 => "[]"
      case _ => "[" + (replies reduceLeft (_ + ", " + _)) + "]"
    }
  }  
}
