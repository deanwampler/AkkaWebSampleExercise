package org.chicagoscala.awse.util.datetime
import org.joda.time._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers


class ToDateTimeTest extends FunSuite with ShouldMatchers {
  import ToDateTime._
  
  test("Converts a Long as milliseconds to a DateTime") {
    1234567890123L.toDateTime match {
      case Some(dt) => dt should equal (new DateTime(1234567890123L))
    }
  }
  
  test("Converts a negative Long to the current DateTime") {
    val neg   = -1234567890123L
    val delta = neg.toDateTime match {
      case Some(dt) => dt.getMillis - (new DateTime).getMillis
      // case None => fail
    }
    math.abs(delta) < 10 should be (true)
  }
  
  test("Converts a String containing a Long as milliseconds to a DateTime") {
    "1234567890123".toDateTime match {
      case Some(dt) => dt should equal (new DateTime(1234567890123L))
      case None => fail
    }
  }
  
  test("Handles a trailing 'L' or 'l' on a millisecond String") {
    "1234567890123L".toDateTime match {
      case Some(dt) => dt should equal (new DateTime(1234567890123L))
      case None => fail
    }
    "1234567890123l".toDateTime match {
      case Some(dt) => dt should equal (new DateTime(1234567890123L))
      case None => fail
    }
  }
  
  test("Converts a String containing a negative Long to the current DateTime") {
    val delta = "-1234567890123".toDateTime match {
      case Some(dt) => dt.getMillis - (new DateTime).getMillis
      case None => fail
    }
    math.abs(delta) < 10 should be (true)
  }
  
  test("Converts a String in a format recognized by the DateTime(Object) constructor") {
    "2011-03-05T09:37:32.059-06:00".toDateTime match {
      case Some(dt) => dt should equal (new DateTime("2011-03-05T09:37:32.059-06:00"))
      case None => fail
    }
  }
  
  class ObjWithDateTimeString(s: String) {
    override def toString = s
  }
  
  test("Converts an Any to a String and attempts to convert the String to a DateTime") {
    new ObjWithDateTimeString("2011-03-05T09:37:32.059-06:00").toDateTime match {
      case Some(dt) => dt should equal (new DateTime("2011-03-05T09:37:32.059-06:00"))
      case None => fail
    }
  }
  
}