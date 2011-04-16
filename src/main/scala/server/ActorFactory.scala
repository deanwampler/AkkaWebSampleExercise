package org.chicagoscala.awse.server
import akka.actor._
import akka.actor.Actor._
import org.chicagoscala.awse.util.Logging

/**
 * Constructor actors.
 */
trait ActorFactory { this: Actor with Logging =>
    
  def getOrMakeActorFor(actorId: String)(makeActor: (String) => Actor): ActorRef = 
    Actors.registry.actorsFor(actorId).toList match {
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
