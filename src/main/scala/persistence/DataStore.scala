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

// /**
//  * Trait that defines the supported options for the "range" method. Each of the derived traits
//  * implement two of the three methods, delegating to the third, "preferred" method.
//  * Note: Why didn't we use default parameters for "maxNum"? Because of mysterious compiler
//  * errors...
//  */ 
// trait RangeQuery {
//   def range(from: Long,     until: Long,     maxNum: Int): Iterable[JSONRecord]
//   def range(from: DateTime, until: DateTime, maxNum: Int = RangeQuery.MAXIMUM_NUMBER_TO_RETURN): Iterable[JSONRecord]
//   def range(from: String,   until: String,   maxNum: Int = RangeQuery.MAXIMUM_NUMBER_TO_RETURN): Iterable[JSONRecord]
// 
//   def range(from: Long,     until: Long): Iterable[JSONRecord]     = range(from, until, RangeQuery.MAXIMUM_NUMBER_TO_RETURN)
//   def range(from: DateTime, until: DateTime): Iterable[JSONRecord] = range(from, until, RangeQuery.MAXIMUM_NUMBER_TO_RETURN)
//   def range(from: String,   until: String): Iterable[JSONRecord]   = range(from, until, RangeQuery.MAXIMUM_NUMBER_TO_RETURN)
// }
// object RangeQuery {
//   val MAXIMUM_NUMBER_TO_RETURN = java.lang.Integer.MAX_VALUE
// }
// 
// trait RangeQueryWith[DT] extends RangeQuery {
//   implicit val ordering: Ordering[DT]
// }
// 
// /**
//  * RangeQuery trait for data stores emphasizing date time queries using DateTime objects.
//  */
// trait RangeQueryWithDateTimes extends RangeQueryWith[DateTime] {
// 
//   def range(from: Long,   until: Long,   maxNum: Int): Iterable[JSONRecord] =
//     range(new DateTime(from), new DateTime(until), maxNum)
//   def range(from: String, until: String, maxNum: Int): Iterable[JSONRecord] =
//     range(new DateTime(from), new DateTime(until), maxNum)
// 
//   implicit val ordering = new Ordering[DateTime] {
//     def compare(dt1: DateTime, dt2: DateTime) = dt1 compareTo dt2
//   }  
// }
// 
// /**
//  * RangeQuery trait for data stores emphasizing date time queries using millisecond Long objects.
//  */
// trait RangeQueryWithMilliseconds extends RangeQueryWith[Long] {
//   
//   def range(from: DateTime, until: DateTime, maxNum: Int): Iterable[JSONRecord] =
//     range(from.getMillis, until.getMillis, maxNum)
//   def range(from: String, until: String, maxNum: Int): Iterable[JSONRecord] =
//   range(new DateTime(from), new DateTime(until), maxNum)
//   
//   // Converting to DateTime arguments in range.
//   implicit def toDT(dateTime: DateTime): Long = dateTime.getMillis
//   implicit def toDT(dateTimeString: String): Long = toDT(new DateTime(dateTimeString))
//   
//   implicit val ordering = Ordering[Long]
// }
// 
// /**
//  * RangeQuery trait for data stores emphasizing date time queries using strings that represent date times.
//  */
// trait RangeQueryWithStrings extends RangeQueryWith[String] {
//   
//   def range(from: Long,   until: Long,   maxNum: Int): Iterable[JSONRecord] =
//     range(toDT(from), toDT(until), maxNum)
//   def range(from: DateTime, until: DateTime, maxNum: Int): Iterable[JSONRecord] =
//     range(toDT(from), toDT(until), maxNum)
// 
//   // Converting to DateTime arguments in range.
//   val dateTimeFormatPattern: String
//   val format = DateTimeFormat.forPattern(dateTimeFormatPattern)
//   def toDT(milliseconds: Long): String = toDT(new DateTime(milliseconds))
//   def toDT(dateTime: DateTime): String = format.print(dateTime)
//   
//   implicit val ordering = Ordering[String]
// }

// trait RangeQuery[DT] {
//   def range(from: DT, until: DT, maxNum: Int = java.lang.Integer.MAX_VALUE): Iterable[JSONRecord]
//   
//   implicit def toDT(milliseconds: Long): DT
//   implicit def toDT(dateTime: DateTime): DT
//   implicit def toDT(dateTimeString: String): DT
// 
//   implicit val ordering: Ordering[DT]
// }
// 
// trait RangeQueryWithDateTimes extends RangeQuery[DateTime] {
// 
//   // Converting to DateTime arguments in range.
//   implicit def toDT(milliseconds: Long): DateTime = new DateTime(milliseconds)
//   implicit def toDT(dateTimeString: String): DateTime = new DateTime(dateTimeString)
//   
//   implicit val ordering = new Ordering[DateTime] {
//     def compare(dt1: DateTime, dt2: DateTime) = dt1 compareTo dt2
//   }
//   
// }
// 
// trait RangeQueryWithMilliseconds extends RangeQuery[Long] {
//   
//   // Converting to DateTime arguments in range.
//   implicit def toDT(dateTime: DateTime): Long = dateTime.getMillis
//   implicit def toDT(dateTimeString: String): Long = toDT(new DateTime(dateTimeString))
//   
//   implicit val ordering = Ordering[Long]
// }
// 
// trait RangeQueryWithStrings extends RangeQuery[String] {
//   
//   // Converting to DateTime arguments in range.
//   val dateTimeFormatPattern: String
//   val format = DateTimeFormat.forPattern(dateTimeFormatPattern)
//   implicit def toDT(milliseconds: Long): String = toDT(new DateTime(milliseconds))
//   implicit def toDT(dateTime: DateTime): String = format.print(dateTime)
//   
//   implicit val ordering = Ordering[String]
// }
