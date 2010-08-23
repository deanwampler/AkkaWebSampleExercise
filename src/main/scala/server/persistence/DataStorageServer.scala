package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence.mongodb._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.stm.Transaction._
import se.scalablesolutions.akka.util.Logging
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import org.joda.time._

/**
 * DataStorageServer manages storage of time-oriented data, stored as JSON.
 */
class DataStorageServer(val service: String) extends Actor with PingHandler with Logging {

  val actorName = "DataStoreServer("+service+")"
      
  log.info("Creating: "+actorName)
  
  def receive = defaultHandler orElse pingHandler
  
  def defaultHandler: PartialFunction[Any, Unit] = {

    case Get(min, max) => 
      self.reply(getData(min, max))
            
    case Put(json) => self.reply(putData(json))

    case Stop => 
      log.ifInfo (actorName + ": Received Stop message.")
      self stop

    case x => 
      val message = actorName + ": unknown message received: " + x
      log.ifInfo (message)
      self.reply (("error", message))
  }
    
  protected[persistence] def getData(min: Long, max: Long) = try {
    val data = for {
      json <- dataStore.range(min, max) // TODO: doesn't work "accurately"
    } yield json
    val result = toJSON(data toList)
    log.ifDebug(actorName + ": GET returning response for min, max, size = " + 
      min + ", " + max + ", " + result.size)
    result
  } catch {
    case th => 
      log.error(actorName + ": Exception thrown: ", th)
      th.printStackTrace
      throw th
  }
  
  protected[persistence] def putData(json: String) = {
    val jsonShortStr = if (json.length > 100) json.substring(0,100) + "..." else json
    log.info(actorName + " PUT: storing Pair(" + jsonShortStr + ")")
      
    try {
      dataStore.add(Pair(json))
      Pair("message", "Put received and data storage started.")
    } catch {
      case ex => 
        log.error(actorName + ": PUT: exception thrown while attempting to add JSON to the data store: "+json)
        ex.printStackTrace();
        throw ex
    }
  }

  protected def toJSON(data: List[String]) = data.size match {
    case 0 => "[]"
    case _ => "[" + data.reduceLeft(_ + ", " + _) + "]"
  }
  
  protected lazy val dataStore = DataStorageServer.makeDefaultDataStore(service)  
}

object DataStorageServer extends Logging {

  import se.scalablesolutions.akka.config.Config.config

  // protected def makeActor(actorName: String): Actor = new DataStorageServer(actorName)

  def getAllDataStorageServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[DataStorageServer]) 

  /**
   * Instantiate the default type of datastore: an InMemoryDataStore with an upper limit on values.
   */
  def makeDefaultDataStore(storeName: String): DataStore[String] = {
    val db = System.getProperty("app.datastore.type", config.getString("app.datastore.type", "mongodb"))
    if (db.toLowerCase.trim == "mongodb") {
      log.ifInfo("Using MongoDB-backed data storage.")
      new MongoDBDataStore(storeName)
    } else {
      log.ifInfo("Using in-memory data storage.")
      new InMemoryDataStore[String](storeName)
    }
  }
}
