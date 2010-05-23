package org.chicagoscala.awse.server
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.util.Logging

// "Pinging" an actor

trait PingableActor extends NamedActor {
  /**
   * Use in receive.
   * e.g., def receive = pingHandler orElse ...
   */
  def handlePing: PartialFunction[Any, Unit] = {
    case Pair("ping", message @ _) => 
      log.debug("DataStorageServerSupervisor received message: "+message)
      reply("{\"message\": \""+actorName+" received ping message "+message+"\"}")
  } 
}
