package org.chicagoscala.awse.persistence.inmemory
import org.chicagoscala.awse.persistence._
import se.scalablesolutions.akka.util.Logging
import scala.collection.SortedMap
import org.joda.time._

/**
 * Pure, in-memory storage of data with no backing store. Note that it may not perform well
 * as the collection grows large.
 * This is largely used only for testing. The MongoDBDataStore provides the features we really
 * need for the "production" app.
 */
class InMemoryDataStore(val name: String) extends DataStore with Logging {

  var store = SortedMap[Long, JSONRecord]()
  
  def add(item: JSONRecord): Unit = store += item.timestamp.getMillis -> item
    
  // def map[T](f: Record => T) = store map f
  
  def getAll() = store map {p => p._2}
  
  def range(from: Long, until: Long, maxNum: Int): Iterable[JSONRecord] = 
    store.range(from, until).map(p => p._2).take(maxNum).toIterable
  
  def size: Long = store.size
}

