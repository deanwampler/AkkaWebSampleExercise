package org.chicagoscala.awse.persistence
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._

class YearMonthDayTimestampTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {
  test("is ordered") {
    val t0 = YearMonthDayTimestamp(2010,01,01)
    val t1 = YearMonthDayTimestamp(2010,01,02)
    val t2 = YearMonthDayTimestamp(2011,01,01)
    val t3 = YearMonthDayTimestamp(2011,02,01)
    List(t2, t3, t0, t1) sortBy (_.toString) should equal (List(t0, t1, t2, t3))
  }
  
  test("can be constructed from a valid string.") {
    YearMonthDayTimestamp("2011 X-_: 01  Xx-_ 01").toString should equal ("2011-01-01")
  }
  
  test("throws an exception when a constructor string is invalid.") {
    intercept[InvalidTimestampString] {
      YearMonthDayTimestamp("foobar")
    }
  }

  test("can be constructed from a DateTime.") {
    val dt = new DateTime("2011-01-01")
    YearMonthDayTimestamp(dt) should equal (YearMonthDayTimestamp(2011,01,01))
  }
  
  test("will ignore the HH:MM:SS in a DateTime.") {
    val dt = new DateTime("2011-01-01T12:34:56")
    YearMonthDayTimestamp(dt) should equal (YearMonthDayTimestamp(2011,01,01))
  }
  
  test("can be constructed from a Long for milliseconds.") {
    val dt = new DateTime(1000000)
    YearMonthDayTimestamp(dt.getMillis) should equal (YearMonthDayTimestamp(dt))
  }
}
