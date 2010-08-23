package org.chicagoscala.awse.persistence
import org.joda.time._

/**
 * Trait for storage of time-oriented data, where the timestamp is in milliseconds (Longs) and
 * it is used as the primary key.
 */
trait DataStore[V] {

  type Record = Pair[DateTime, V]
  
  def add(item: Record): Unit
  
  def addAll(items: Iterable[Record]): Unit = for (item <- items) add(item)
  
  def map[T](f: Record => T): Iterable[T]
  
  def getAll(): Iterable[Record]
  
  def range(startAt: DateTime, upTo: DateTime): Iterable[Record]

  def size: Int
}
