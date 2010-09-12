package org.chicagoscala.awse.persistence
import org.joda.time._
import org.joda.time.format._
import scala.math._
import scala.math.Ordering._

/**
 * Trait for storage of time-oriented, JSON data, parameterized by the format used for date times in
 * queries.
 */
trait DataStore {

  def add(item: JSONRecord): Unit
  
  def addAll(items: Iterable[JSONRecord]): Unit = for (item <- items) add(item)
  
  def getAll(): Iterable[JSONRecord]
  
  def range(from: DateTime, until: DateTime, maxNum: Int = java.lang.Integer.MAX_VALUE): Iterable[JSONRecord]

  def size: Long
}  
