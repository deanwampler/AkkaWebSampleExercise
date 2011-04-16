package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.util.Logging
import org.chicagoscala.awse.util.error
import akka.config.Config.config
import scala.collection.immutable.SortedSet
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import com.mongodb.{BasicDBObject, BasicDBList, DBCursor, Mongo, MongoException, QueryBuilder}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._

import MongoDBDataStore.Implicits._

/**
 * MongoDB-based storage of data, using the Casbah API. 
 * Note the implicit dateTimeToTimestamp, which converts DateTime objects 
 * into the correct type  for the actual records. It defaults to milliseconds.
 */
class MongoDBDataStore[T: ValidDateOrNumericType](
    val collectionName: String,
    val dataBaseName: String      = MongoDBDataStore.MONGODB_SERVER_DBNAME,
    val hostName: String          = MongoDBDataStore.MONGODB_SERVER_HOSTNAME,
    val port: Int                 = MongoDBDataStore.MONGODB_SERVER_PORT)
   (dateTimeToTimestamp: DateTime => T) 
    extends DataStore with Logging {

  lazy val name = collectionName
  
  lazy val connection = MongoConnection(hostName, port)
  lazy val dataBase   = connection(dataBaseName)
  lazy val collection = dataBase(collectionName)
  
  def add(record: JSONRecord): Unit = collection += record.toMap
  
  def getAll() = (for (record <- collection.find()) yield(JSONRecord(record.toMap))).toList

  def size: Long = collection.count
  
  def head: Option[JSONRecord] = collection.headOption match {
    case None => None
    case Some(dbo) => Some(JSONRecord(dbo.toMap))
  }
  
  def range(from: DateTime, to: DateTime, otherCriteria: Map[String,Any] = Map.empty, maxNum: Int): Iterable[JSONRecord] = try {
    val rangeQuery: DBObject = JSONRecord.timestampKey $gte dateTimeToTimestamp(from) $lte dateTimeToTimestamp(to)
		val otherQueryParameters = for {
			keyValue <- otherCriteria
			criterium <- toCriterium(keyValue)
		} yield (criterium)
		val query: DBObject = otherQueryParameters.foldLeft(rangeQuery) {(query,dbo) => query ++ dbo}
    val cursor = collection.find(query).sort(MongoDBObject(JSONRecord.timestampKey -> 1))
    log.info("db name: query, cursor.count, maxNum: "+collection.getFullName+", "+
              query+", "+cursor.count+", "+maxNum)
    val cursor2 = if (cursor.count <= maxNum) cursor else cursor.skip(cursor.count - maxNum).limit(maxNum)
    (for (record <- cursor2) yield (JSONRecord(record.toMap))).toList
  } catch {
    case th => error(th, "MongoDB Exception while during range().")
  }

	def toCriterium(keyValue: Pair[_,_]): Option[DBObject] = keyValue._1 match {
		case key:String =>
			keyValue._2 match {
				case list: List[_] => Some(key $in list)
        case map: Map[_,_] => error("Construction of a query with a Map is TBD.")
        case x: Any => Some(MongoDBObject(key -> x))
      }
    case x => error("Unexpected key "+x+". Wasn't a string.")
	}
  
  def getDistinctValuesFor(keyForValues: String): Iterable[JSONRecord] = try {
    List(JSONRecord(Map(keyForValues -> collection.distinct(keyForValues).toList)))
  } catch {
    case th => error(th, "MongoDB Exception in getDistinctValuesFor().")
  }
}

object MongoDBDataStore {
	object Implicits {
		implicit val validStringDate = new ValidDateOrNumericType[String] {}
	}

	//RegisterJodaTimeConversionHelpers()

  val mongodbConfigPrefix = "akka.remote.server.server.client.storage.mongodb"
  val MONGODB_SERVER_HOSTNAME = config.getString(mongodbConfigPrefix+".hostname", "127.0.0.1")
  val MONGODB_SERVER_DBNAME = config.getString(mongodbConfigPrefix+".dbname", "statistics")
  val MONGODB_SERVER_PORT = config.getInt(mongodbConfigPrefix+".port", 27017)
  
  def getDb(
      dbName: String   = MONGODB_SERVER_DBNAME,
      hostName: String = MONGODB_SERVER_HOSTNAME,
      port: Int        = MONGODB_SERVER_PORT) = 
    new Mongo(hostName, port).getDB(dbName)
}

