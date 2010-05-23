package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config.OneForOneStrategy
import se.scalablesolutions.akka.util.Logging

class DataStorageServerSupervisor extends Actor with ActorSupervision with PingableActor {
  trapExit = List(classOf[Throwable])
  faultHandler = Some(OneForOneStrategy(5, 5000))
  lifeCycle = Some(LifeCycle(Permanent))
  
  val actorName = "DataStorageServerSupervisor"

  protected def makeActor(actorName: String): Actor = new DataStorageServer(actorName)
  
  def receive = handleManagementMessage orElse handlePing
}

object DataStorageServerSupervisor extends Logging {
    
  lazy val instance = new DataStorageServerSupervisor

  def getAllDataStorageServers: List[Actor] = 
    ActorRegistry.actorsFor(classOf[DataStorageServer]) 
}
  
