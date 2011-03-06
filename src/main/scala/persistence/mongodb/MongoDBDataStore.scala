package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.mongodb.MongoDBJSONRecord._
import org.chicagoscala.awse.util.Logging
import org.chicagoscala.awse.util.error
import akka.config.Config.config
import scala.collection.immutable.SortedSet
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import com.osinka.mongodb._
import com.mongodb.{BasicDBObject, BasicDBList, DBCursor, Mongo, MongoException, QueryBuilder}
// import com.novus.casbah.mongodb.Imports._

/**
 * MongoDB-based storage of data. 
 * Some sections adapted from http://gist.github.com/370577. 
 * Other sections adapted from Akka's own MongoStorageBackend.scala.
 * Note the implicit dateTimeToTimestamp, which converts DateTime objects 
 * into the correct type  for the actual records. It defaults to milliseconds.
 */
class MongoDBDataStore(
    val collectionName: String,
    val dataBaseName: String      = MongoDBDataStore.MONGODB_SERVER_DBNAME,
    val hostName: String          = MongoDBDataStore.MONGODB_SERVER_HOSTNAME,
    val port: Int                 = MongoDBDataStore.MONGODB_SERVER_PORT)
   (implicit dateTimeToTimestamp: DateTime => Any = {_.getMillis}) 
    extends DataStore with Logging {

  lazy val name = collectionName
  
  lazy val dataBase = 
    MongoDBDataStore.getDb(dataBaseName, hostName, port)
    
  // The MongoDB Java API documentation lies: createCollection throws an exception
  // if the collection already exists. So, we catch it and call getCollection.
  lazy val collection = try {
    val coll = dataBase.createCollection(collectionName, Map.empty[String,Any])  // options
    // We setup indices when we create the collections; otherwise, do this:
    // coll ensureIndex Map(JSONRecord.timestampKey -> 1)
    coll asScala
  } catch {
    case ex: MongoException => 
      log.info("MongoException thrown, probably because we called createCollection on a collection that exists, in which case this is harmless (and contrary to the documentation...): "+ex)
      dataBase.getCollection(collectionName) asScala
  }
  
  def add(record: JSONRecord): Unit = collection << record
  
  def getAll() = cursorToRecords(collection.find())

  def size: Long = collection.underlying.getCount
  
  def head: Option[JSONRecord] = collection.headOption match {
    case None => None
    case Some(dbo) => Some(JSONRecord(dbo.toMap))
  }
  
  def range(from: DateTime, to: DateTime, otherCriteria: Map[String,Any] = Map.empty, maxNum: Int): Iterable[JSONRecord] = try {
    val qb = new QueryBuilder
    qb.and(JSONRecord.timestampKey).
      greaterThanEquals(dateTimeToTimestamp(from)).
      lessThanEquals(dateTimeToTimestamp(to))
    // Add the additional query criteria, if any.
    otherCriteria.foreach { (keyValue: Pair[String,Any]) =>
      keyValue._2 match {
        case list: List[_] => 
          val dbList = list.foldLeft(new BasicDBList()) { (dbl: BasicDBList, x:Any) => 
            dbl.add(x.asInstanceOf[AnyRef])
            dbl
          }
          qb.and(keyValue._1).in(dbList)
        case map: Map[_,_] => error("Construction of a query with a Map is TODO.")
        case x: AnyRef => qb.is(x)
        case x: Any => error("Construction of a query with an Any is TODO.")
      }
    }
    val query = qb.get
    val cursor = collection.find(query).sort(new BasicDBObject(JSONRecord.timestampKey, 1))
    log.info("db name: query, cursor.count, maxNum: "+collection.getFullName+", "+
              query+", "+cursor.count+", "+maxNum)
    if (cursor.count > maxNum)
      cursorToRecords(cursor.skip(cursor.count - maxNum).limit(maxNum))
    else
      cursorToRecords(cursor)
  } catch {
    case th => error(th, "MongoDB Exception while during range().")
  }
  
  def getDistinctValuesFor(keyForValues: String): Iterable[JSONRecord] = try {
    val list = collection.distinct(keyForValues)
    val buff = new scala.collection.mutable.ArrayBuffer[String]()
    var iter = list.iterator
    while (iter.hasNext) {
      buff += iter.next.toString
    }
    List(JSONRecord(keyForValues -> buff.toList))
  } catch {
    case th => error(th, "MongoDB Exception while during getDistinctValuesFor().")
  }
  
  protected def cursorToRecords(cursor: DBCursor) = {
    val buff = new scala.collection.mutable.ArrayBuffer[JSONRecord]()
    while (cursor.hasNext) {
      buff += JSONRecord(cursor.next.toMap)
    }
    buff
  }
}

object MongoDBDataStore extends Logging {
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

