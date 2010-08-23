package org.chicagoscala.awse.server
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.util.Logging


sealed trait ActorManagementMessage
case class GetActorFor(name: String)    extends ActorManagementMessage
case class Register(actor: ActorRef)    extends ActorManagementMessage
case class Unregister(actor: ActorRef)  extends ActorManagementMessage

trait ActorSupervision extends Actor with Logging {
  
  def handleManagementMessage: PartialFunction[Any,Unit] = {
    case Register(actor)   => doRegister(actor)

    case Unregister(actor) => doUnregister(actor)
  }

  protected def doRegister(actor: ActorRef) = {
    log.ifInfo("Registering actor: "+actor)
    self link actor
    Pair("message", "Actor registered")
  }

  def doUnregister(actor: ActorRef) = {
    log.ifInfo("Registering actor: "+actor)
    self unlink actor
    Pair("message", "Actor unregistered")
  }
}

object ActorSupervision {
  
  def getOrMakeActorFor(actorId: String, supervisorForMadeActor: Option[ActorRef])(makeActor: (String) => ActorRef) = 
    ActorRegistry.actorsFor(actorId) match {
      case Nil => 
        log.ifInfo("Creating new actor: "+actorId)
        val actorRef = makeActor(actorId)
        actorRef.id = actorId
        actorRef.start
        supervisorForMadeActor match {
          case None => log.warning("No supervisor specified for new actor " + actorId)
          case Some(s) => s ! Register(actorRef)
        }
        actorRef
      case head :: tail => 
        if (tail != Nil) log.warning("More than one actor exists with id " + actorId)
        log.ifInfo("Returning existing actor " + actorId)
        head
    }
}
