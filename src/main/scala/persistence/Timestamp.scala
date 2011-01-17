package org.chicagoscala.awse.persistence
import org.joda.time._
import org.joda.time.format._

/**
 * Encapsulates a Year-Month-Day timestamp with conversion from DateTimes and
 * validation of timestamp strings.
 */

object YearMonthDayFormatter {
  def apply(dateTime: DateTime): String = {
    val formatter = new DateTimeFormatterBuilder()
        .appendYear(4, 4)
        .appendLiteral('-')
        .appendMonthOfYear(2)
        .appendLiteral('-')
        .appendDayOfMonth(2)
        .toFormatter();
    formatter.print(dateTime)
  }
}

case class InvalidTimestampString(time: String) 
  extends RuntimeException("Invalid timestamp: "+time+". Expected format: YYYY-MM-DD")
  
case class YearMonthDayTimestamp(year: Int, month: Int, day: Int) extends Ordered[YearMonthDayTimestamp] {
  def compare(other: YearMonthDayTimestamp) = year - other.year match {
    case 0 => month - other.month match {
      case 0 => day - other.day
      case n => n
    }
    case n => n
  }
  
  override def toString = "%04d-%02d-%02d" format (year, month, day)
}

object YearMonthDayTimestamp {
  val regex = """(\d{4})\D+(\d{2})\D+(\d{2})""".r

  def isValid(time: String) = time match {
    case regex(year, month, day) => true
    case _ => false
  }
  
  def apply(time: String): YearMonthDayTimestamp = time match {
    case regex(year, month, day) => 
      new YearMonthDayTimestamp(Integer.parseInt(year),Integer.parseInt(month),Integer.parseInt(day))
    case _ => throw InvalidTimestampString(time)
  }

  /**
   * Will discard HH:MM:SS!
   */
  def apply(time: DateTime): YearMonthDayTimestamp = 
    new YearMonthDayTimestamp(time.getYear, time.getMonthOfYear, time.getDayOfMonth)

  /**
   * Will discard HH:MM:SS!
   */
  def apply(millis: Long): YearMonthDayTimestamp = apply(new DateTime(millis))
}
