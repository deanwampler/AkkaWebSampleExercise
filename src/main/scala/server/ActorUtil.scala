package org.chicagoscala.awse.server
import se.scalablesolutions.akka.dispatch.{Future, Futures}
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

/**
 * A trait to add "utility" methods to actors, such as extracting JSON from Futures.
 */
trait ActorUtil {

  /**
   * When an unknown message is received, return a uniform error message.
   */
  def unrecognizedMessageHandler: PartialFunction[Any, Unit] = {
    case message => Pair("error", "Unrecognized message received: " + message)
  }
  
  /**
   * Process the result of a list of Actor futures in a uniform way.
   */
   def futuresToJSON(futures: List[Future[_]], messageForNone: String): JValue = {
     val replies = for { future <- futures } yield futureToJSON(future, messageForNone)
     replies.size match {
       case 0 => JNothing
       case _ => replies reduceLeft (_ ++ _)
     }    
   }
   
  /**
   * Process the result of a single Actor future in a uniform way.
   */
  def futureToJSON(future: Future[_], messageForNone: String): JValue = future.result match {
    case Some(result) => result match {
      case jv: JValue => jv
      case _ => throw new RuntimeException("Expected a JValue, got this: "+result)
    }
    case None => Pair("error", messageForNone)
  }
}
