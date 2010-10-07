package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor.ActorRef
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

  def makeJSON(list: List[JSONRecord]): JValue = list reduceLeft (_ ++ _) json
  
  def makeJSONString(json: JValue): String = compact(render(json))

  def makeCriteria(instruments: String, stats: String, start: Long, end: Long) = 
    CriteriaMap().
      withInstruments(instruments).
      withStatistics(stats).
      withStart(start). 
      withEnd(end)

  def makeExpected(json: JValue, criteria: CriteriaMap) = 
    analysisServer.formatPriceResults(
      json, criteria.instruments, criteria.statistics, criteria.start, criteria.end)
  
  def sendAndWait(msg: Message): Option[String] = {
    answer = (driverActor !!! msg).await.result
    answer
  }
  
  def loadData = js.reverse foreach ((jsr: JSONRecord) => sendAndWait(Put(jsr)))

  var analysisServer: InstrumentAnalysisServerHelper = _
  var testDataStore: InMemoryDataStore = _
  var dss: ActorRef = _
  var driverActor: ActorRef = _
  var answer: Option[String] = None

  override def beforeEach = {
    testDataStore = new InMemoryDataStore("testDataStore")
    dss = actorOf(new DataStorageServer("testService", testDataStore))
    analysisServer = new InstrumentAnalysisServerHelper(dss) 
    driverActor = actorOf(new Actor {
      def receive = {
        case msg => (dss !!! msg).await.result match {
          case Some(s) => self.reply(s)
          case None => fail(msg.toString)
        }
      }
    })
    dss.start
    driverActor.start
    loadData
  }
  override def afterEach = {
    driverActor.stop
    dss.stop
  }

  test ("calculateStatistics returns a JSON string containing all data when all data matches the query criteria") {
    val criteria = makeCriteria("A,B,C", "price", 0, nowms)
    val expected = makeExpected(makeJSON((List(js(0), js(3), js(2), js(4), js(1)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  test ("calculateStatistics returns a JSON string containing all data that matches the time criteria") {
    // Return all data for the specified time range, low to high (inclusive)
    val criteria = makeCriteria("A,B,C", "price", thenms + 1000, thenms + 3000)
    val expected = makeExpected(makeJSON((List(js(3), js(2), js(4)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  test ("The time criteria are inclusive for the earliest time and the latest time") {
    val criteria = makeCriteria("A,B,C", "price", thenms + 1000, thenms + 3000)
    val expected = makeExpected(makeJSON((List(js(3), js(2), js(4)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  // TODO
  test ("calculateStatistics returns a JSON string containing all data that matches the instrument criteria") {
    pending
    val criteria = makeCriteria("A", "price", 0, nowms)
    val expected = makeExpected(makeJSON((List(js(0), js(1), js(2)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  // TODO
  test ("calculateStatistics returns a JSON string containing all data that matches the statistics criteria") {
    pending
    val criteria1 = makeCriteria("A,B,C", "price", 0, nowms)
    val criteria2 = makeCriteria("A,B,C", "50dma", 0, nowms)
    val expected1 = makeExpected(makeJSON((List(js(0), js(3), js(2), js(4), js(1)))), criteria1)
    val expected2 = makeExpected(makeJSON((List(js(0), js(3), js(2), js(4), js(1)))), criteria2)
    analysisServer.calculateStatistics(criteria1) should equal (expected1)
    analysisServer.calculateStatistics(criteria2) should equal (expected2)
  }
}
