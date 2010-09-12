package org.chicagoscala.awse.persistence
import org.scalatest.{FlatSpec, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

abstract class DataStoreTestBase extends FunSuite with ShouldMatchers {

  type DS <: DataStore

  var dataStore: DS
  
  def makeTR(timestamp: Long, value: Long) = JSONRecord(("timestamp" -> timestamp) ~ ("value" -> value))
    
  val start = (new DateTime).getMillis

  test("A new DataStore should have no elements") {
    dataStore.size should equal (0)
  }

  def add3Elements = {
    dataStore add makeTR(101L, 3000)
    dataStore add makeTR(42L,  1000)
    dataStore add makeTR(57L,  2000)
  }
  
  test("After adding N elements, a DataStore should have N elements") {
    add3Elements
    dataStore.size should equal (3)
  }

  test("If empty, getAll should returns zero elements") {
    dataStore.getAll.size should equal (0)
  }

  test("After adding N elements, getAll should return the same N elements, sorted by the Long timestamp") {
    add3Elements
    dataStore.getAll.toList zip (List(makeTR(42L, 1000), makeTR(57L, 2000), makeTR(101L, 3000))) map {
      pair => pair._1 equalsIgnoringId pair._2
    }
  }
  
  def populateDataStore(size: Long, offset: Long = 7L) = {
    for (i <- offset   until (size + offset) by 2L) 
      { dataStore add makeTR(i, (i*10L)) }
    for (i <- offset+1 until (size + offset) by 2L)
      { dataStore add makeTR(i, (i*10L)) }
  }
  
  test("range returns a subset of a DataStore from a starting bounds upto but not including an upper bound should return a Traversable with the correct subset") {
    populateDataStore(100)
    val range = dataStore.range(new DateTime(20L), new DateTime(25L)).toList
    range zip (List(makeTR(20L, 200), makeTR(21L, 210), makeTR(22L, 220), makeTR(23L, 230), makeTR(24L, 240))) map {
      pair => pair._1 equalsIgnoringId pair._2
    }
  }
  
  test("range returns a subset of a DataStore from a starting bounds upto but not including an upper bound, with a maximum number of values to return") {
    populateDataStore(100)
    val range = dataStore.range(new DateTime(20L), new DateTime(50L), 7).toList
    range.size should equal (7)
    range zip (List(makeTR(23L, 230), makeTR(27L, 270), makeTR(31L, 310), makeTR(35L, 350), makeTR(39L, 390), makeTR(43L, 430), makeTR(47L, 470))) map {
      pair => pair._1 equalsIgnoringId pair._2
    }
    
    val range2 = dataStore.range(new DateTime(20L), new DateTime(49L), 7).toList
    range2.size should equal (7)
    range2 zip (List(makeTR(23L, 230), makeTR(27L, 270), makeTR(31L, 310), makeTR(35L, 350), makeTR(39L, 390), makeTR(43L, 430), makeTR(47L, 470))) map {
      pair => pair._1 equalsIgnoringId pair._2
    }
  }
  
  test("range returns all of the data in the range if the maximum number is greater than the size of the data set") {
    populateDataStore(100)
    val range = dataStore.range(new DateTime(20L), new DateTime(50L), 1000).toList
    range.size should equal (30)
    def testList(l:List[_], expectedN:Int):Unit = l match {
      case Nil =>
      case head :: tail => head match {
        case jsr: JSONRecord => 
          jsr equalsIgnoringId makeTR(expectedN, expectedN*10L) should be (true)
          testList(tail, expectedN+1)
        case _ => fail("expected pair, but got: "+head)
      }
    }
    testList(range, 20)
  }
  
  test("If the start is < 0, range uses 0. If the until is > size, range uses size" ) {
    populateDataStore(100, 0)
    val range = dataStore.range(new DateTime(-1L), new DateTime(101L)).toList
    range.size should equal (100)
    range.foreach { rec => 
      val ts = rec.timestamp.getMillis
      rec equalsIgnoringId makeTR(ts, (ts*10L)) should be (true) 
    }
    val range2 = dataStore.range(new DateTime(-100L), new DateTime(200L)).toList
    range2.size should equal (100)
    range2.foreach { rec => 
      val ts = rec.timestamp.getMillis
      rec equalsIgnoringId makeTR(ts, (ts*10L)) should be (true) 
    }
  }
  
  test("range returns an empty equence if the start is >= the end") {
    populateDataStore(100, 0)
    for (n <- List(0L, 10L, 99L)) {
      val range1 = dataStore.range(new DateTime(n), new DateTime(n))
      range1.size should equal (0)
      val range2 = dataStore.range(new DateTime(n+1), new DateTime(n))
      range2.size should equal (0)
    }
  }

  test("for big data set, range returns expected data") {
    populateDataStore(20000, 0)
    val range1 = dataStore.range(new DateTime(-10), new DateTime(10000))
    range1.size should equal (10000)
    range1.head equalsIgnoringId makeTR(0,0) should be (true)
    range1.last equalsIgnoringId makeTR(9999,99990) should be (true)
    val range2 = dataStore.range(new DateTime(-10), new DateTime(100000), java.lang.Integer.MAX_VALUE)
    range2.size should equal (20000)
    range2.head equalsIgnoringId makeTR(0,0) should be (true)
    range2.last equalsIgnoringId makeTR(19999,199990) should be (true)
  }
  
}
