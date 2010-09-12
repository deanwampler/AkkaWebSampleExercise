package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence.mongodb._
import org.chicagoscala.awse.util._
import org.chicagoscala.awse.util.json.JSONMap._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.stm.Transaction._
import se.scalablesolutions.akka.util.Logging
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import org.joda.time._

case class CouldNotFindDateTime(key: String, json: JValue) extends RuntimeException(
  "Could not find expected date time field for key "+key+" in JSON "+compact(render(json)))
  
/**
 * DataStorageServer manages access to time-oriented data, stored as JSON.
 * TODO: Currently, the query capabilities are limited to date-time range queries.
 */
class DataStorageServer(val serviceName: String, val dataStore: DataStore) 
    extends Transactor with PingHandler with Logging {

  val actorName = "DataStoreServer("+serviceName+")"

  log.info("Creating: "+actorName)
  
  /**
   * The message handler calls its own "defaultHandler" first. If it doesn't match on the
   * message (because it is a PartialFunction), then the "pingHandler" is tried.
   */
  def receive = pingHandler orElse defaultHandler 
  
  def defaultHandler: PartialFunction[Any, Unit] = {

    case Get(criteria) => self.reply(getData(criteria))
            
    case Put(jsonRecord) => self.reply(putData(jsonRecord))

    case Stop => 
      log.info (actorName + ": Received Stop message.")
      self stop

    case x => 
      val message = actorName + ": unknown message received: " + x
      log.info (message)
      self.reply (toJValue(Pair("error", message)))
  }
  
  // TODO: Support other query criteria besides time ranges.
  protected[persistence] def getData(criteria: JValue): JValue = {
    log.debug(actorName + ": GET starting...")
    val start: DateTime = extractTime(criteria, "start", new DateTime(0))
    val end: DateTime   = extractTime(criteria, "end",   new DateTime)
    try {
      val data = for {
        json <- dataStore.range(start, end)
      } yield json
      val result = toJSON(data toList)
      log.debug(actorName + ": GET returning response for start, end = " + 
        start + ", " + end)
      result
    } catch {
      case th => 
        log.error(actorName + ": Exception thrown: ", th)
        th.printStackTrace
        throw th
    }
  }

  protected[persistence] def putData(jsonRecord: JSONRecord) = {
    log.debug(actorName + " PUT: storing JSON: " + jsonShortStr(jsonRecord.toString))
    try {
      dataStore.add(jsonRecord)
      toJValue(Pair("message", "Put received and data storage started."))
    } catch {
      case ex => 
        log.error(actorName + ": PUT: exception thrown while attempting to add JSON to the data store: "+jsonRecord)
        ex.printStackTrace();
        throw ex
    }
  }

  protected def extractTime(json: JValue, key: String, default: => DateTime): DateTime = (json \ key) match {
    case JField(key, value) => value match {
      case JInt(millis) => new DateTime(millis.toLong)
      case JString(s) => new DateTime(s)
      case _ => default
    }
    case _ => default
  } 

  // TODO: Use JSONMap.toJValue instead.
  protected def toJSON(data: List[JSONRecord]): JValue = data.size match {
    case 0 => JNothing
    case _ => data reduceLeft { _ ++ _ } json
  }
  
  private def jsonShortStr(jstr: String) = 
    if (jstr.length > 100) jstr.substring(0,100) + "..." else jstr
}

object DataStorageServer extends Logging {

  def getAllDataStorageServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[DataStorageServer]).toList 
}
