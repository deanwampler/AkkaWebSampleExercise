package org.chicagoscala.awse.domain.finance
import org.scalatest.{FlatSpec, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._

class CriteriaMapTest extends FunSuite with ShouldMatchers {

  def checkInstruments(cm: CriteriaMap) = cm.instruments match {
    case Nil => fail("No instruments! ")
    case list => 
      list.size should equal (3)
      list match {
        case i1 :: i2 :: i3 :: Nil =>
          i1 should equal (Instrument("a"))
          i2 should equal (Instrument("b"))
          i3 should equal (Instrument("c"))
        case _ => fail(list.toString)
      }
  }

  def checkStatistics(cm: CriteriaMap) = cm.statistics match {
    case Nil => fail("No statistics! ")
    case list => 
      list.size should equal (3)
      list match {
        case i1 :: i2 :: i3 :: head =>
          i1 should equal (Price(Dollars))
          i2 should equal (Price(Dollars))
          i3 should equal (MovingAverage(50))
        case _ => fail(list.toString)
      }
  }

  // Some tests may be off by up to 10 milliseconds
  def checkTime(actual: DateTime, expected: DateTime) =
    (math.abs(actual.getMillis - expected.getMillis) < 10) should be (true)

  def checkStart(cm: CriteriaMap, expected: DateTime) = checkTime(cm.start, expected)

  def checkEnd(cm: CriteriaMap, expected: DateTime) = checkTime(cm.end, expected)
  
  test ("withInstruments(String) adds a list of Instruments to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withInstruments("a,b,c")
    cm.map.size should equal (1)
    checkInstruments(cm)
  }
  test ("withInstruments(List[Instrument]) adds a list of Instruments to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withInstruments(Instrument.makeInstrumentsList("a,b,c"))
    cm.map.size should equal (1)
    checkInstruments(cm)
  }
  test ("withInstruments(Instrument*) adds a list of Instruments to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withInstruments(Instrument("a"), Instrument("b"), Instrument("c"))
    cm.map.size should equal (1)
    checkInstruments(cm)
  }

  test ("withStatistics(String) adds a list of InstrumentStatistics to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStatistics("price,price[$],50dma")
    cm.map.size should equal (1)
    checkStatistics(cm)
  }
  test ("withStatistics(List[InstrumentStatistic]) adds a list of InstrumentStatistics to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStatistics(InstrumentStatistic.makeInstrumentStatisticsList("price,price[$],50dma"))
    cm.map.size should equal (1)
    checkStatistics(cm)
  }
  test ("withStatistics(InstrumentStatistic*) adds a list of InstrumentStatistics to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStatistics(InstrumentStatistic("price"), InstrumentStatistic("price[$]"), InstrumentStatistic("50dma"))
    cm.map.size should equal (1)
    checkStatistics(cm)
  }

  val now = new DateTime
  val nowms  = now.getMillis
  val thenms = nowms - 10000
  val then = new DateTime(thenms)
  
  test ("withStart(String) adds a start time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStart("2010-10-30")
    cm.map.size should equal (1)
    checkStart(cm, new DateTime("2010-10-30"))
  }
  test ("withStart(\"\") adds the current time as the start time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStart("")
    cm.map.size should equal (1)
    checkStart(cm, new DateTime())
  }
  test ("withStart(bogus string) throws an exception") {
    intercept[CriteriaMap.InvalidTimeString] {
      CriteriaMap(Map()).withStart("x")
    }
  }
  test ("withStart(Long) adds a start time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStart(thenms)
    cm.map.size should equal (1)
    checkStart(cm, then)
  }
  test ("withStart(DateTime) adds a start time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStart(then)
    cm.map.size should equal (1)
    checkStart(cm, then)
  }
  
  test ("withEnd(String) adds a end time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withEnd("2010-10-30")
    cm.map.size should equal (1)
    checkEnd(cm, new DateTime("2010-10-30"))
  }
  test ("withEnd(\"\") adds the current time as the end time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withEnd("")
    cm.map.size should equal (1)
    checkEnd(cm, new DateTime())
  }
  test ("withEnd(bogus string) throws an exception") {
    intercept[CriteriaMap.InvalidTimeString] {
      CriteriaMap(Map()).withEnd("x")
    }
  }
  test ("withEnd(Long) adds a end time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withEnd(thenms)
    cm.map.size should equal (1)
    checkEnd(cm, then)
  }
  test ("withEnd(DateTime) adds a end time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withEnd(then)
    cm.map.size should equal (1)
    checkEnd(cm, then)
  }
  
  test ("unapply extracts the fields from a CriteriaMap") {
    val start = CriteriaMap.defaultStartTime
    val end   = CriteriaMap.defaultEndTime
    val cm = CriteriaMap(Map()).withStart(start).withEnd(end).withInstruments("a,b,c").withStatistics("price,price[$],50dma")
    cm match {
      case CriteriaMap(instruments, statistics, start1, end1) => 
        checkInstruments(cm)
        checkStatistics(cm)
        checkStart(cm, start1)
        checkEnd(cm, end1)
      case _ => fail(cm.toString)
    }
  }
}