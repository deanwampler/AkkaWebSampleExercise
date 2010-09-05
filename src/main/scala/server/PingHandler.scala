package org.chicagoscala.awse.server
import org.chicagoscala.awse.util.json.JSONMap._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.dispatch.{Future, Futures, FutureTimeoutException}
import se.scalablesolutions.akka.util.Logging
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

case class Ping(message: String)

/** 
 * Logic for "pinging" a hierarchy of actors. It is useful for ensuring liveness,
 * debugging, and for returning a list of the actors that are currently alive.
 */
trait PingHandler extends Actor with ActorUtil {
  
  /**
   * Return a list of "subordinates" to ping. An actor that supervises other 
   * actors should override this method to return the list of those actors.
   */
  protected def subordinatesToPing: List[ActorRef] = Nil
  
  /**
   * Handle ping-related messages. Use in the the receive method:
   * e.g., def receive = pingHandler orElse ...
   * TODO: If there is only one actor running, an array with that actor is returned. If there
   * are multiple actors, an array of an array is returned ([[actor1, actor2, ...]]). Fix so only
   * a single-level array is always returned!
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