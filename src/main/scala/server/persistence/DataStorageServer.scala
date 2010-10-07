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
  
case class InvalidCriteria(message: String, criteria: JValue) extends RuntimeException(
  "The specified criteria was not valid. "+message+". criteria: "+compact(render(criteria)))

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

  // TODO: a little messy...
  protected[persistence] def getData(criteria: Map[String, Any]): JValue = criteria.get("instrument_list") match {
    case Some(x) => x match {
      case prefix: String => criteria.get("instrument_symbols_key") match {
        case Some(y) => y match {
          case keyForInstruments: String => getInstrumentList(prefix, keyForInstruments)
          case _ => throw new InvalidCriteria(
            "Map contained key-value pairs for keys 'instrument_list' and 'instrument_symbols_key', but the value for 'instrument_symbols_key' was not a string: value = ", y)
        }          
        case _ => throw new InvalidCriteria(
          "Map contained a key-value pair for key 'instrument_list', but not for key 'instrument_symbols_key': criteria = ", criteria)
      }
      case _ => throw new InvalidCriteria(
        "Map contained a key-value pair for key 'instrument_list', but the value was not a string: value = ", x)
    }
    case _ => getDataForRange(criteria)    
  }
  
  // TODO: Support other query criteria besides time ranges.
  protected[persistence] def getDataForRange(criteria: Map[String, Any]): JValue = {
    log.debug(actorName + ": Starting getDataForRange:")
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

  // Hack!
  protected[persistence] def getInstrumentList(prefix: String, keyForInstrumentSymbols: String): JValue = {
    log.debug(actorName + ": Starting getInstrumentList for prefix = "+prefix+" and symbol key = "+keyForInstrumentSymbols)
    dataStore match {
      case mongo: MongoDBDataStore => 
        val data = for {
					json <- mongo.getInstrumentList(prefix, keyForInstrumentSymbols)
        } yield json
        val result = toJSON(data toList)
        log.info("DataStorageServer.getInstrumentList returning: "+result)
        result
      case _ => throw new RuntimeException("Can't get the instrument list from datastore "+dataStore)
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

  protected def extractTime(criteria: Map[String, Any], key: String, default: => DateTime): DateTime = criteria.get(key) match {
    case Some(value) => value match {
      case dt:     DateTime => dt
      case millis: Long     => new DateTime(millis)
      case s:      String   => new DateTime(s)
      case _                => default
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
