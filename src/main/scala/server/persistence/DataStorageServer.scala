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

    case Get(fromTime, untilTime) => 
      self.reply(getData(fromTime, untilTime))
            
    case Put(time, json) => self.reply(putData(time, json))

    case Stop => 
      log.ifInfo (actorName + ": Received Stop message.")
      self stop

    case x => 
      val message = actorName + ": unknown message received: " + x
      log.ifInfo (message)
      self.reply (("error", message))
  }
    
  protected[persistence] def getData(fromTime: DateTime, untilTime: DateTime) = try {
    val data = for {
      (timeStamp, json) <- dataStore.range(fromTime, untilTime)
    } yield json
    val result = toJSON(data toList)
    log.ifDebug(actorName + ": GET returning response for startTime, endTime, size = " + 
      fromTime + ", " + untilTime + ", " + result.size)
    result
  } catch {
    case th => 
      log.error(actorName + ": Exception thrown: ", th)
      th.printStackTrace
      throw th
  }
  
  protected[persistence] def putData(time: DateTime, json: String) = {
    val jsonShortStr = if (json.length > 100) json.substring(0,100) + "..." else json
    log.info(actorName + " PUT: storing Pair(" + time + ", " + jsonShortStr + ")")
      
    try {
      dataStore.add(Pair(time, json))
      Pair("message", "Put received for time " + time + ". Data storage started.")
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
