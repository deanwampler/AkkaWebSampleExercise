package org.chicagoscala.awse.server
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging

// "Pinging" an actor

trait PingHandler extends Actor {
  
  /**
   * Override if you want to do any additional work...
   */
  protected def afterPing(ping: Pair[String,String]) = ""
  
  /**
   * Use in receive.
   * e.g., def receive = pingHandler orElse ...
   */
  def pingHandler: PartialFunction[Any, Unit] = {
    case Pair("ping", message @ _) => 
      log.debug("Ping message: "+message)
      val afterPingResponse = afterPing(Pair("ping", message.toString))
      self.reply("{\"pong\": \"" + this + "\". " + afterPingResponse + "}")
  } 
}
