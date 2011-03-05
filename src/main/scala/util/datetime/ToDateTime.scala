package org.chicagoscala.awse.util.datetime
import org.joda.time._

trait ToDateTime {
  def toDateTime: Option[DateTime]
}

object ToDateTime {
  case class InvalidDateTime(value: Any) extends RuntimeException("Invalid date time: "+value)
  
  implicit def now = new DateTime
  
  implicit def fromMillis(millis: Long) = MillisToDateTime(millis)
  implicit def fromString(s: String) = StringToDateTime(s)
  implicit def fromAmu(a: Any) = AnyToDateTime(a)

  /**
   * Convert a long to a DateTime, treating the long as the epoch milliseconds.
   * Treats a negative value as a "flag" to use the current time.
   */
  case class MillisToDateTime(millis: Long) extends ToDateTime {
    def toDateTime = Some(if (millis < 0) (new DateTime) else (new DateTime(millis)))
  }
  
  /**
   * Convert a string to a DateTime, if the string is in a format recognized
   * by the DateTime(Object) constructor.
   */ 
  case class StringToDateTime(dateTimeString: String) extends ToDateTime {
    def toDateTime = 
      tryBlank(dateTimeString) orElse 
      tryLong(dateTimeString) orElse 
      tryNonEmptyString(dateTimeString)

    protected def tryBlank(candidate: String) = candidate.trim match {
      case "" => Some(new DateTime)
      case _  => None
    }

    /**
     * Try parsing a string as a long, removing a trailing 'L' or 'l', which
     * appear to trip up java.lang.Long.parseLong(s).
     */
    protected def tryLong(candidate: String) = try {
      val noL = candidate.replaceAll("[Ll]", "")
      MillisToDateTime(java.lang.Long.parseLong(noL)).toDateTime
    } catch {
      case ex => None
    }

    protected def tryNonEmptyString(candidate: String) = try {
      Some(new DateTime(candidate))
    } catch {
      case ex => None
    } 
  }

  /**
   * Convert any other object to a string and attempt to convert the string to a DateTime.
   */
  case class AnyToDateTime(a: Any) extends ToDateTime {
    def toDateTime = StringToDateTime(a.toString).toDateTime
  }
  
}

