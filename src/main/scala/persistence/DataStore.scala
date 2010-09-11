package org.chicagoscala.awse.persistence
import org.joda.time._

/**
 * Trait for storage of time-oriented, JSON data.
 */
trait DataStore {

  def add(item: JSONRecord): Unit
  
  def addAll(items: Iterable[JSONRecord]): Unit = for (item <- items) add(item)
  
  def getAll(): Iterable[JSONRecord]
  
  // Why not define two methods with default values for the maxNum argument? For some reason, we get
  // a strange type check error when we call range(x,y) elsewhere. The following avoids this problem.
  // TODO: Refactor which time format gets preference!
  def range(from: DateTime, until: DateTime): Iterable[JSONRecord] =
    range(from.getMillis, until.getMillis)

  def range(from: DateTime, until: DateTime, maxNum: Int): Iterable[JSONRecord] =
    range(from.getMillis, until.getMillis, maxNum)
    
  def range(from: Long, until: Long): Iterable[JSONRecord] = range(from, until, java.lang.Integer.MAX_VALUE)

  def range(from: Long, until: Long, maxNum: Int): Iterable[JSONRecord]

  def size: Long
}
