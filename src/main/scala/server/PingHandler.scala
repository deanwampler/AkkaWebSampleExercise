package org.chicagoscala.awse.server
import org.chicagoscala.awse.util.json.JSONMap._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.dispatch.{Future, Futures, FutureTimeoutException}
import se.scalablesolutions.akka.util.Logging
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

// "Pinging" an actor

case class Ping(message: String)

trait PingHandler extends Actor with ActorUtil {
  
  /**
   * Return a list of "subordinates" to ping. Override if there is
   * a nonempty list.
   */
  protected def subordinatesToPing: List[ActorRef] = Nil
  
  /**
   * Use in receive.
   * e.g., def receive = pingHandler orElse ...
   */
  def pingHandler: PartialFunction[Any, Unit] = {
    case Ping(message) => 
      log.debug("Ping message: "+message)
      val fullResponse = toJValue(this.toString) ++ pingSubordinates(subordinatesToPing, message)
      self reply fullResponse
  } 
  
  protected def pingSubordinates(actors: List[ActorRef], message: String): JValue = actors match {
    case Nil => JNothing
    case _ => 
      try {
        val futures = actors map { _ !!! Ping(message) }
        Futures.awaitAll(futures)
        handlePingRepliesIn(futures)
      } catch {
        case fte: FutureTimeoutException =>
          Pair("error", "Actors timed out (" + fte.getMessage + ")")
      }
  }
    
  def handlePingRepliesIn(futures: Iterable[Future[_]]): JValue =
    futuresToJSON(futures toList, "failed!")
}  