package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.persistence._
import se.scalablesolutions.akka.persistence.mongo._
import se.scalablesolutions.akka.util.Logging
import scala.collection.immutable.SortedSet
import org.joda.time._

/**
 * MongoDB-based storage of data with timestamps as keys.
 */
class MongoDBDataStore(name: String) extends DataStore[String] with Logging {

  // Store the timestamps, used as keys, in an in-memory sorted set so we can keep the records sorted.
  var recordKeys = SortedSet[DateTime]()(DateTimeOrdering)
  
  val recordMap = MongoStorage.newMap(name)
  
  def add(item: Record): Unit = {
    recordMap  += (new DateTime(item._1) -> item)
    recordKeys += item._1
  }
    
  def map[T](f: Record => T) = recordKeys map { key => f(recordMap(key).asInstanceOf[Record]) }
  
  def getAll() = recordKeys map { key => recordMap(key).asInstanceOf[Record] }

  def size: Int = recordKeys size
  
  def head: Option[Record] = recordKeys.headOption match {
    case None => None
    case Some(key) => Some(recordMap(key.asInstanceOf[DateTime]).asInstanceOf[Record])
  }
  
  // TODO: Use Mongo's Query capabilities.
  def range(fromTime: DateTime, untilTime: DateTime) = {
    val ftime = fromTime.getMillis
    val utime = untilTime.getMillis
    recordKeys filter { time => ftime <= time.getMillis && time.getMillis < utime } map { 
      key => recordMap(key).asInstanceOf[Record] 
    }
  }
}
