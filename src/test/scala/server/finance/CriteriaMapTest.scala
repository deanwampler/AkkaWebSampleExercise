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
        case i1 :: i2 :: i3 =>
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
        case i1 :: i2 :: i3 =>
          i1 should equal (Price(Dollars))
          i2 should equal (Price(Dollars))
          i3 should equal (MovingAverage(50))
        case _ => fail(list.toString)
      }
  }

  def checkStart(cm: CriteriaMap, expected: DateTime) = cm.start should equal (expected)

  def checkEnd(cm: CriteriaMap, expected: DateTime) = cm.end should equal (expected)
  
  // def checkTime(cm: CriteriaMap, whichTime: String, expected: DateTime) = cm.map.get(whichTime) match {
  //   case Some(dateTime) => 
  //     dateTime should equal (expected)
  //   case _ => fail("No " + whichTime + " time! ")
  // }

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
  val then = now.minus(new Duration(1000))
  val nowms  = now.getMillis
  val thenms = then.getMillis
  
  test ("withStart(String) adds a start time to a CriteriaMap") {
    val cm = CriteriaMap(Map()).withStart("August 30 2010")
    cm.map.size should equal (1)
    checkStart(cm, new DateTime("August 30 2010"))
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
    val cm = CriteriaMap(Map()).withEnd("August 30 2010")
    cm.map.size should equal (1)
    checkEnd(cm, new DateTime("August 30 2010"))
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
    val cm = CriteriaMap(Map()).withStart(start).withEnd(end).withInstruments("a,b,c").withStatistics("s1,s2,s3")
    cm match {
      case CriteriaMap(instruments, statistics, start1, end1) => 
        instruments should equal (Instrument.makeInstrumentsList("a,b,c"))
        statistics  should equal (InstrumentStatistic.makeInstrumentStatisticsList("s1,s2,s3"))
        start1      should equal (start)
        end1        should equal (end)
      case _ => fail(cm.toString)
    }
  }
  test ("unapply extracts the fields from a Map") {
    val start = CriteriaMap.defaultStartTime
    val end   = CriteriaMap.defaultEndTime
    val cm = CriteriaMap(Map()).withStart(start).withEnd(end).withInstruments("a,b,c").withStatistics("s1,s2,s3")
    cm.map match {
      case CriteriaMap(instruments, statistics, start1, end1) => 
        instruments should equal (Instrument.makeInstrumentsList("a,b,c"))
        statistics  should equal (InstrumentStatistic.makeInstrumentStatisticsList("s1,s2,s3"))
        start1      should equal (start)
        end1        should equal (end)
      case _ => fail(cm.toString)
    }
  }
}