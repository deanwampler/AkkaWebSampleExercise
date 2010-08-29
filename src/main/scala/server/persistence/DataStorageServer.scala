package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence.mongodb._
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.util._
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

  protected lazy val dataStore = DataStorageServer.makeDefaultDataStore(service)  

  log.info("Creating: "+actorName)
  
  def receive = defaultHandler orElse pingHandler
  
  def defaultHandler: PartialFunction[Any, Unit] = {

    case Get(criteria) => self.reply(getData(criteria))
            
    case Put(jsonRecord) => self.reply(putData(jsonRecord))

    case Stop => 
      log.ifInfo (actorName + ": Received Stop message.")
      self stop

    case x => 
      val message = actorName + ": unknown message received: " + x
      log.ifInfo (message)
      self.reply (("error", message))
  }
   
  protected[persistence] def getData(criteria: Map[String,Any]) = try {
    val startingAt  = extractDate(criteria, "startingAt", 0)
    val upTo        = extractDate(criteria, "upTo", (new DateTime).getMillis)
    val instruments = extractListOf[Instrument](criteria, "instruments", s => Instrument(s))
    val statistics  = extractListOf[InstrumentStatistic](criteria, "statistics", s => InstrumentStatistic.make(s))
    val data = for {
      json       <- dataStore.range(startingAt, upTo)
      // TODO: Select by instrument, stats.
      // instrument <- instruments
      // statistic  <- statistics
    } yield json
    val result = toJSONString(data toList)
    log.ifDebug(actorName + ": GET returning response for start, end, size = " + 
      startingAt + ", " + upTo + ", " + result.size)
    result
  } catch {
    case th => 
      log.error(actorName + ": Exception thrown: ", th)
      th.printStackTrace
      throw th
  }
  
  protected[persistence] def putData(jsonRecord: JSONRecord) = {
    log.ifInfo(actorName + " PUT: storing Pair(" + jsonShortStr(jsonRecord.toString) + ")")
      
    try {
      dataStore.add(jsonRecord)
      Pair("message", "Put received and data storage started.")
    } catch {
      case ex => 
        log.error(actorName + ": PUT: exception thrown while attempting to add JSON to the data store: "+jsonRecord)
        ex.printStackTrace();
        throw ex
    }
  }

  protected def toJSONString(data: List[JSONRecord]): String = data.size match {
    case 0 => "{}"
    case _ => compact(render(data reduceLeft { _ ++ _ } json))
  }
  
  protected def extractDate(criteria: Map[String,Any], key: String, default: Long): Long = criteria.get(key) match {
    case None => default
    case Some(x) => x match {
      case millis: Long => millis
      case dt: DateTime => dt.getMillis
      case s: String => (new DateTime(s)).getMillis
      case _ => error("Unrecognized object specified for date time '"+key+"'")
    }
  }
  
  protected def extractListOf[T](
      criteria: Map[String,Any], key: String, makeT: String => T): List[T] = {
    def makeListOf(list: List[_], accum: List[T]): List[T] = list match {
      case Nil => accum
      case head :: tail => 
        val i:T = head match {
          case s: String => makeT(s)
          case _ => head.asInstanceOf[T]  // TODO: use manifest to make this safer.
        } 
        makeListOf(tail, i :: accum)
    }
    criteria.get(key) match {
      case None => Nil
      case Some(x) => x match {
        case list: List[_] => makeListOf(list, Nil)
        case _ => error("Unrecognized object specified for '"+key+"'")
      }
    }
  }
  
  private def jsonShortStr(jstr: String) = 
    if (jstr.length > 100) jstr.substring(0,100) + "..." else jstr
}

object DataStorageServer extends Logging {

  import se.scalablesolutions.akka.config.Config.config

  // protected def makeActor(actorName: String): Actor = new DataStorageServer(actorName)

  def getAllDataStorageServers: List[ActorRef] = 
    ActorRegistry.actorsFor(classOf[DataStorageServer]) 

  /**
   * Instantiate the default type of datastore: an InMemoryDataStore with an upper limit on values.
   */
  def makeDefaultDataStore(storeName: String): DataStore[JSONRecord] = {
    val db = System.getProperty("app.datastore.type", config.getString("app.datastore.type", "mongodb"))
    if (db.toLowerCase.trim == "mongodb") {
      log.ifInfo("Using MongoDB-backed data storage.")
      new MongoDBDataStore(storeName)
    } else {
      log.ifInfo("Using in-memory data storage.")
      new InMemoryDataStore[JSONRecord](storeName)
    }
  }
}
