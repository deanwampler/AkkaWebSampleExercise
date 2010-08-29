package org.chicagoscala.awse.persistence
import org.joda.time._

/**
 * Trait for storage of time-oriented data, where the timestamp is in milliseconds (Longs) and
 * it is used as the primary key.
 */
trait DataStore[Record <: RecordWithTimestamp] {

  def add(item: Record): Unit
  
  def addAll(items: Iterable[Record]): Unit = for (item <- items) add(item)
  
  // def map[T](f: Record => T): Iterable[T]
  
  def getAll(): Iterable[Record]
  
  // Why not define two methods with default values for the maxNum argument? For some reason, we get
  // a strange type check error when we call range(x,y) elsewhere. The following avoids this problem.
  
  def range(from: DateTime, until: DateTime): Iterable[Record] =
    range(from.getMillis, until.getMillis)

  def range(from: DateTime, until: DateTime, maxNum: Int): Iterable[Record] =
    range(from.getMillis, until.getMillis, maxNum)
    
  def range(from: Long, until: Long): Iterable[Record] = range(from, until, java.lang.Integer.MAX_VALUE)

  def range(from: Long, until: Long, maxNum: Int): Iterable[Record]

  def size: Long
}
