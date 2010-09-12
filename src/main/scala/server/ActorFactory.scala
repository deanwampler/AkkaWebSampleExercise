package org.chicagoscala.awse.server
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging

/**
 * Constructor actors.
 */
trait ActorFactory { this: Actor with Logging =>
    
  def getOrMakeActorFor(actorId: String)(makeActor: (String) => Actor): ActorRef = 
    ActorRegistry.actorsFor(actorId).toList match {
      case Nil => 
        log.info("Created new actor with id "+actorId+".")
        val actorRef = actorOf(makeActor(actorId))
        actorRef.id = actorId
        manageNewActor(actorRef)
      case head :: tail => 
        if (tail != Nil) log.warning("More than one actor exists with id " + actorId)
        log.info("Returning existing actor " + actorId)
        head
    }

  def manageNewActor(actorRef: ActorRef): ActorRef = {
    actorRef.start
    log.info("Registering actor: "+actorRef.id)
    self link actorRef
    actorRef
  }
}
