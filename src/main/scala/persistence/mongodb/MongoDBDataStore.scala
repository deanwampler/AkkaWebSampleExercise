package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.mongodb.MongoDBJSONRecord._
import se.scalablesolutions.akka.config.Config.config
import se.scalablesolutions.akka.util.Logging
import scala.collection.immutable.SortedSet
import org.joda.time._
import com.osinka.mongodb._
import com.mongodb.{BasicDBObject, DBCursor, Mongo, MongoException}

/**
 * MongoDB-based storage of data. 
 * Some sections adapted from http://gist.github.com/370577. 
 * Other sections adapted from Akka's own MongoStorageBackend.scala.
 */
class MongoDBDataStore(
    val collectionName: String,
    val dataBaseName: String      = MongoDBDataStore.MONGODB_SERVER_DBNAME,
    val hostName: String          = MongoDBDataStore.MONGODB_SERVER_HOSTNAME,
    val port: Int                 = MongoDBDataStore.MONGODB_SERVER_PORT)
      extends DataStore[JSONRecord] with Logging {

  lazy val name = collectionName
  
  lazy val dataBase = 
    MongoDBDataStore.getDb(dataBaseName, hostName, port)
    
  // The MongoDB Java API documentation lies: createCollection throws an exception if the collection
  // already exists. So, we catch it and call getCollection.
  lazy val collection = try {
    val coll = dataBase.createCollection(collectionName, Map.empty[String,Any])  // options
    coll ensureIndex Map("timestamp" -> 1)
    coll asScala
  } catch {
    case ex: MongoException => 
      log.info("MongoException thrown, probably because we called createCollection on a collection that exists, in which case this is harmless (and contrary to the documentation...): "+ex)
      dataBase.getCollection(collectionName) asScala
  }
  
  def add(record: JSONRecord): Unit = collection << record
  
  // def map[T](f: JSONRecord => T) = 
  //   collection map { dbo => f(JSONRecord(dbo.toMap)) } toIterable
  
  def getAll() = cursorToRecords(collection.find())

  def size: Long = collection.underlying.getCount
  
  def head: Option[JSONRecord] = collection.headOption match {
    case None => None
    case Some(dbo) => Some(JSONRecord(dbo.toMap))
  }
  
  def range(from: Long, until: Long, maxNum: Int): Iterable[JSONRecord] = try {
    val query = new BasicDBObject()
    query.put("timestamp", new BasicDBObject("$gte", from).append("$lt", until))

    val cursor = collection.find(query).sort(new BasicDBObject("timestamp", 1))
    if (cursor.count > maxNum)
      cursorToRecords(cursor.skip(cursor.count - maxNum).limit(maxNum))
    else
      cursorToRecords(cursor)
  } catch {
    case th => 
      log.error("MongoDB Exception: ", th)
      throw th
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
  val MONGODB_SERVER_HOSTNAME = config.getString("akka.storage.mongodb.hostname", "127.0.0.1")
  val MONGODB_SERVER_DBNAME = config.getString("akka.storage.mongodb.dbname", "statistics")
  val MONGODB_SERVER_PORT = config.getInt("akka.storage.mongodb.port", 27017)
  
  def getDb(
      dbName: String   = MONGODB_SERVER_DBNAME,
      hostName: String = MONGODB_SERVER_HOSTNAME,
      port: Int        = MONGODB_SERVER_PORT) = 
    new Mongo(hostName, port).getDB(dbName)
}

