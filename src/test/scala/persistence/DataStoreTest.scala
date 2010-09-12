package org.chicagoscala.awse.persistence
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.server.persistence._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.{ShouldMatchers}
import org.joda.time._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._

class DataStoreTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  var dataStore: InMemoryDataStore = _
  
  val now   = (new DateTime).getMillis
  val start = (new DateTime(now - 3600)).getMillis

  val js1 = makeJSONRecord(start + 1000,  "one")
  val js2 = makeJSONRecord(start + 2000,  "two")
  val js3 = makeJSONRecord(start + 3000,  "three")

  override def beforeEach {
    dataStore = new InMemoryDataStore("testDataStore")
  }
  
  test("A new DataStore should have no elements") {
    dataStore.size should equal (0)
  }

  def makeJSONRecord(time: Long, s: String) = JSONRecord(("timestamp" -> time) ~ ("string" -> s))
  
  // out of order in time
  def add3Elements = List(js3, js1, js2) foreach (dataStore add _)
  
  test("After adding N elements, a DataStore should have N elements") {
    add3Elements
    dataStore.size should equal (3)
  }

  test("If empty, getAll should return the zero elements") {
    dataStore.getAll.size should equal (0)
  }

  test("After adding N elements, getAll should return the same N elements, sorted by the Long timestamp") {
    add3Elements
    dataStore.getAll.toList should equal (List(js1, js2, js3))
  }
  
  def populateDataStore(size: Long) = {
    for (i <- start      until (start + size ) by 2L) 
      { dataStore add makeJSONRecord(i, "" + (i*10L)) }
    for (i <- start + 1L until (start + size ) by 2L)
      { dataStore add makeJSONRecord(i, "" + (i*10L)) }
  }
  
  test("range returns a subset of a DataStore from a starting bounds upto but not including an upper bound should return a Traversable with the correct subset") {
    populateDataStore(100)
    val range = dataStore.range(new DateTime(start+20), new DateTime(start+25)).toList
    range.size should equal(5)
    def checkEach(i: Int, range2: List[JSONRecord]):Unit = range2 match {
      case Nil =>
      case head :: tail => 
        head.timestamp should equal (new DateTime(start+i+20))
        checkEach(i+1, tail)
    }
    checkEach(0, range)
  }
    
  test("range returns an empty sequence if the start is >= the end") {
    populateDataStore(100)
    for (n <- List(0L, 10L, 99L)) {
      val range1 = dataStore.range(new DateTime(n), new DateTime(n))
      range1.size should equal (0)
      val range2 = dataStore.range(new DateTime(n+1), new DateTime(n))
      range2.size should equal (0)
    }
  }
}
