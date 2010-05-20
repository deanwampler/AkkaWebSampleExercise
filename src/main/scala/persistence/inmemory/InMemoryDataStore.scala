package org.chicagoscala.awse.persistence.inmemory
import org.chicagoscala.awse.persistence._
import se.scalablesolutions.akka.util.Logging
import scala.collection.SortedMap
import org.joda.time._

/**
 * Pure, in-memory storage of data with no backing store. Note that it may not perform well
 * as the collection grows large.
 */
class InMemoryDataStore[V](val name: String) extends DataStore[V] with Logging {

  var store = SortedMap[DateTime,V]()(new scala.math.Ordering[DateTime] {
    def compare(d1: DateTime, d2: DateTime) = d1.getMillis compare d2.getMillis
  }) 
  
  def add(item: Record): Unit = store += item
    
  def map[T](f: Record => T) = (store map f).toIndexedSeq
  
  def getAll() = store.toIndexedSeq
  
  def range(start: DateTime, end: DateTime) = store.range(start, end).toIndexedSeq
  
  def size: Int = store.size
}

