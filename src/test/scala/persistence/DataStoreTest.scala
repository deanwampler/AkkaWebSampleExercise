package org.chicagoscala.awse.persistence
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.server.persistence._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.{ShouldMatchers}
import org.joda.time._

class DataStoreTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  var dataStore: InMemoryDataStore[String] = _
  
  val now   = (new DateTime).getMillis
  val start = (new DateTime(now - 3600)).getMillis

  override def beforeEach {
    dataStore = new InMemoryDataStore[String]("testDataStore")
  }
  
  test("A new DataStore should have no elements") {
    dataStore.size should equal (0)
  }

  def add3Elements = {
    // out of order in time
    dataStore add Pair(new DateTime(start+3000), "three")
    dataStore add Pair(new DateTime(start+1000),  "one")
    dataStore add Pair(new DateTime(start+2000),  "two")
  }
  
  test("After adding N elements, a DataStore should have N elements") {
    add3Elements
    dataStore.size should equal (3)
  }

  test("If empty, getAll should return the zero elements") {
    dataStore.getAll.size should equal (0)
  }

  test("After adding N elements, getAll should return the same N elements, sorted by the Long timestamp") {
    add3Elements
    dataStore.getAll.toList should equal (
      List(Pair(new DateTime(start+1000), "one"), 
           Pair(new DateTime(start+2000), "two"), 
           Pair(new DateTime(start+3000), "three")))
  }
  
  def populateDataStore(size: Long) = {
    for (i <- start      until (start + size ) by 2L) 
      { dataStore add Pair(new DateTime(i), "" + (i*10L)) }
    for (i <- start + 1L until (start + size ) by 2L)
      { dataStore add Pair(new DateTime(i), "" + (i*10L)) }
  }
  
  test("range returns a subset of a DataStore from a starting bounds upto but not including an upper bound should return a Traversable with the correct subset") {
    populateDataStore(100)
    val range = dataStore.range(new DateTime(start+20), new DateTime(start+25)).toList
    range.size should equal(5)
    def checkEach(i: Int, range2: List[Pair[DateTime,_]]):Unit = range2 match {
      case Nil =>
      case head :: tail => 
        head._1 should equal (new DateTime(start+i+20))
        checkEach(i+1, tail)
    }
    checkEach(0, range)
  }
    
  test("range returns an empty equence if the start is >= the end") {
    populateDataStore(100)
    for (n <- List(0L, 10L, 99L)) {
      val range1 = dataStore.range(new DateTime(n), new DateTime(n))
      range1.size should equal (0)
      val range2 = dataStore.range(new DateTime(n+1), new DateTime(n))
      range2.size should equal (0)
    }
  }
}
