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

  implicit val dateTimeOrdering = new scala.math.Ordering[DateTime] {
    def compare(dt1: DateTime, dt2: DateTime) = dt1 compareTo dt2
  }
  
  var store = SortedMap[DateTime, JSONRecord]()
  
  def add(item: JSONRecord): Unit = store += Pair(item.timestamp, item)
    
  def getAll() = store map {p => p._2}
  
  def range(from: DateTime, until: DateTime, maxNum: Int): Iterable[JSONRecord] = 
    store.range(from, until).map(p => p._2).take(maxNum).toIterable
  
  def size: Long = store.size
}

