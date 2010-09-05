package org.chicagoscala.awse.server
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging

/**
 * Methods to assist in the dynamic creation and supervision of subordinate actors.
 */
trait ActorSupervision extends Actor { this:Logging =>
    
  def getOrMakeActorFor(actorId: String)(makeActor: (String) => Actor): ActorRef = 
    ActorRegistry.actorsFor(actorId).toList match {
      case Nil => 
        log.info("Created new actor with id "+actorId+".")
        val actorRef = actorOf(makeActor(actorId))
        actorRef.id = actorId
        actorRef.start
        log.info("Registering actor.")
        self link actorRef
        log.info("Returning new actor.")
        actorRef
      case head :: tail => 
        if (tail != Nil) log.warning("More than one actor exists with id " + actorId)
        log.info("Returning existing actor " + actorId)
        head
    }
}
