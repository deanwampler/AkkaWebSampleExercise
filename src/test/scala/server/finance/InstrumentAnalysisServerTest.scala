package org.chicagoscala.awse.server.finance
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence._
import akka.actor._
import akka.actor.Actor._
import akka.actor.ActorRef
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
    makeJSONRecord(thenms,        "AX",  0.0),
    makeJSONRecord(thenms + 4000, "AY", 40.0),
    makeJSONRecord(thenms + 2000, "AZ", 20.0),
    makeJSONRecord(thenms + 1000, "CZ", 10.0),
    makeJSONRecord(thenms + 3000, "BZ", 30.0))


  def makeJSONRecord(time: Long, symbol: String, price: Double) = {
    val json = ("timestamp" -> time) ~ ("stock_symbol" -> symbol) ~ ("price" -> price)
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
  var testDivDataStore: InMemoryDataStore = _
  var dss: ActorRef = _
  var ddss: ActorRef = _
  var driverActor: ActorRef = _
  var answer: Option[String] = None

  override def beforeEach = {
    testDataStore = new InMemoryDataStore("testDataStore")
    dss = actorOf(new DataStorageServer("testService", testDataStore))
    testDivDataStore = new InMemoryDataStore("testDataStoreForDividends")
    ddss = actorOf(new DataStorageServer("testServiceForDividends", testDivDataStore))
    analysisServer = new InstrumentAnalysisServerHelper(dss, ddss) 
    driverActor = actorOf(new Actor {
      def receive = {
        case msg => (dss !!! msg).await.result match {
          case Some(s) => self.reply(s)
          case None => fail(msg.toString)
        }
      }
    })
    dss.start
    ddss.start
    driverActor.start
    loadData
  }
  override def afterEach = {
    driverActor.stop
    dss.stop
    ddss.stop
  }

  test ("calculateStatistics returns a JSON string containing all data when all data matches the query criteria") {
    val criteria = makeCriteria("AX,AY,AZ,BZ,CZ", "price", 0, nowms)
    val expected = makeExpected(makeJSON((List(js(0), js(3), js(2), js(4), js(1)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  test ("calculateStatistics returns a JSON string containing all data that matches the time criteria") {
    // Return all data for the specified time range, low to high (inclusive)
    val criteria = makeCriteria("AX,AY,AZ,BZ,CZ", "price", thenms + 1000, thenms + 3000)
    val expected = makeExpected(makeJSON((List(js(3), js(2), js(4)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  test ("The time criteria are inclusive for the earliest time and the latest time") {
    val criteria = makeCriteria("AX,AY,AZ,BZ,CZ", "price", thenms + 1000, thenms + 3000)
    val expected = makeExpected(makeJSON((List(js(3), js(2), js(4)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  test ("calculateStatistics returns a JSON string containing all data that matches the instrument criteria") {
    val criteria = makeCriteria("AX,AY,AZ", "price", 0, nowms)
    val expected = makeExpected(makeJSON((List(js(0), js(1), js(2)))), criteria)
    analysisServer.calculateStatistics(criteria) should equal (expected)
  }

  test ("calculateStatistics returns a JSON string containing all data that matches the statistics criteria") {
    val criteria1 = makeCriteria("AX,AY,AZ,BZ,CZ", "price", 0, nowms)
    val criteria2 = makeCriteria("AX,AY,AZ,BZ,CZ", "50dma", 0, nowms)
    val expected1 = makeExpected(makeJSON((List(js(0), js(3), js(2), js(4), js(1)))), criteria1)
    val expected2 = makeExpected(makeJSON((List(js(0), js(3), js(2), js(4), js(1)))), criteria2)
    analysisServer.calculateStatistics(criteria1) should equal (expected1)
    analysisServer.calculateStatistics(criteria2) should equal (expected2)
  }
  
  // TODO: In fact, all the instruments in the corresponding datastore are returned.
  // The datastores are structured to only store symbols beginning with a particular letter.
  test ("getInstrumentList returns a list of all the instruments, independent of the input starting letter") {
    analysisServer.getInstrumentList(List('A'), "stock_symbol") \ "stock_symbol" match {
      case JField("stock_symbol", array) => array.values should equal (List("AX", "AY", "AZ", "BZ", "CZ"))
      case _ => fail
    }
  }
}
