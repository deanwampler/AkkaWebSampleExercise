package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.util.Logging

object DataStorageServerSupervisor extends Logging {

  protected def makeActor(actorName: String): Actor = new DataStorageServer(actorName)
  
  def getAllDataStorageServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[DataStorageServer]) 
}
  
