package org.chicagoscala.awse.persistence
import org.joda.time._

/**
 * Trait for storage of time-oriented data, where the timestamp is in milliseconds (Longs).
 */
trait DataStore[V] {

  type Record = Pair[DateTime,V]
  
  def add(item: Record): Unit
  
  def addAll(items: Iterable[Record]): Unit = for (item <- items) add(item)
  
  def map[T](f: Record => T): Iterable[T]
  
  def getAll(): Iterable[Record]
  
  /** 
   * Return a collection of elements within the specified range.
   * @param fromTime  Long timestamp inclusive that is the first element in the pairs being stored, not the index into this datastructure.
   * @param untilTime Long timestamp exclusive that is the first element in the pairs being stored, not the index into this datastructure.
   */
  def range(fromTime: DateTime, untilTime: DateTime): Iterable[Record]

  def size: Int
}
