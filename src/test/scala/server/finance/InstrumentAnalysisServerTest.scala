package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence._
import se.scalablesolutions.akka.actor.ActorRef
import se.scalablesolutions.akka.actor.Actor._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

class InstrumentAnalysisServerTest extends FunSuite 
    with ShouldMatchers with BeforeAndAfterEach {

  val now          = new DateTime
  val nowms        = now.getMillis
  val thenms       = nowms - 10000
  val then         = new DateTime(nowms - 10000)
  val epochStart   = new DateTime(0)

  val js = List(
    makeJSONRecord(thenms,        "A",  0.0),
    makeJSONRecord(thenms + 4000, "A", 40.0),
    makeJSONRecord(thenms + 2000, "A", 20.0),
    makeJSONRecord(thenms + 1000, "C", 10.0),
    makeJSONRecord(thenms + 3000, "B", 30.0))

  def makeJSONRecord(time: Long, symbol: String, price: Double) = {
    val json = ("timestamp" -> time) ~ ("symbol" -> symbol) ~ ("price" -> price)
    JSONRecord(json)
  }

  def makeJSONString(list: List[JSONRecord]) = {
    val s = list reduceLeft (_ ++ _)
    "[" + s.toJSONString + "]"
  }

  def makeCriteria(instruments: String, stats: String, start: Long, end: Long) = 
    CriteriaMap().
      withInstruments(instruments).
      withStatistics(stats).
      withStart(start). 
      withEnd(end)
  
  def loadData = js.reverse foreach (dss !! Put(_))

  var analysisServer: InstrumentAnalysisServerHelper = _
  var testDataStore: InMemoryDataStore[JSONRecord] = _
  var dss: ActorRef = _

  override def beforeEach = {
    testDataStore = new InMemoryDataStore[JSONRecord]("testDataStore")
    dss = actorOf(new DataStorageServer("testService") {
      override lazy val dataStore = testDataStore 
    })
    analysisServer = new InstrumentAnalysisServerHelper(dss) 
    dss.start
    loadData
  }
  override def afterEach = {
    dss.stop
  }

  test ("calculateStatistics returns a JSON string containing all data when all data matches the query criteria") {
    val result = makeJSONString (List(js(0), js(3), js(2), js(4), js(1)))
    analysisServer.calculateStatistics(makeCriteria("A,B,C","price", 0, nowms)) should equal (result)
  }

  test ("calculateStatistics returns a JSON string containing all data that matches the time criteria") {
    // Return all data for the specified time range, low (inclusive) to high (exclusive)
    val result = makeJSONString (List(js(3), js(2), js(4)))
    analysisServer.calculateStatistics(makeCriteria("A,B,C" , "price" , thenms + 1000, thenms + 3001)) should equal (result)
  }

  test ("The time criteria are inclusive for the earliest time and exclusive for the latest time") {
    val result = makeJSONString (List(js(3), js(2)))
    analysisServer.calculateStatistics(makeCriteria("A,B,C" , "price" , thenms + 1000, thenms + 3000)) should equal (result)
  }

  // TODO
  test ("calculateStatistics returns a JSON string containing all data that matches the instrument criteria") {
    pending
    val result = makeJSONString (List(js(0), js(2), js(4)))
    analysisServer.calculateStatistics(makeCriteria("A", "price", 0, -1)) should equal (result)
  }

  // TODO
  test ("calculateStatistics returns a JSON string containing all data that matches the statistics criteria") {
    pending
    val result = makeJSONString (List(js(0), js(3), js(2), js(4), js(1)))
    analysisServer.calculateStatistics(makeCriteria("A,B,C", "price", 0, -1)) should equal (result)
    analysisServer.calculateStatistics(makeCriteria("A,B,C", "50dma", 0, -1)) should equal (Nil)
  }

}
