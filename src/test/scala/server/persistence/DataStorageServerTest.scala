package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.domain.finance._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory.InMemoryDataStore
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import org.joda.time.format._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

class DataStorageServerTest extends FunSuite 
    with ShouldMatchers with BeforeAndAfterEach {
  
  val epochStart = 0
  val now        = new DateTime().getMillis
  val thenms     = now - 100000
  
  def makeJSONRecord(time: Long, symbol: String, price: Double) = 
    JSONRecord(("timestamp" -> time) ~ ("symbol" -> symbol) ~ ("price" -> price))
  
  def makeJSONString(list: JSONRecord*) = (list reduceLeft (_ ++ _)) toJSONString

  def populateDataStore(server: ActorRef, numberOfItems:Int) = {
    val data = for {
      i <- 0 until numberOfItems
      time = thenms + (1000L * i)
      json = makeJSONRecord(time, "A", i * 10.0)
    } yield (server !! Put(json))
  }
  
  def makeGet(instruments: List[String], statistics: List[String], startingAt: Long, upTo: Long) =
    Get(Map(
      "instruments" -> Instrument.makeInstrumentsList(instruments),
      "statistics" -> InstrumentStatistic.makeStatisticsList(statistics),
      "startingAt" -> startingAt,
      "upTo" -> upTo))
  
  var testDataStore: InMemoryDataStore[JSONRecord] = _
  var dss: ActorRef = _

  override def beforeEach = {
    testDataStore = new InMemoryDataStore[JSONRecord]("testDataStore")
    dss = actorOf(new DataStorageServer("testService") {
      override lazy val dataStore = testDataStore 
    })
    dss.start
  }
  override def afterEach = {
    dss.stop
  }
  
  test("Get message should return an empty JSON string if there is no data") {

    val response1: Option[String] = dss !! (makeGet(List("A"), List("price"), epochStart, now), 10000)
    response1.get should equal ("{}")
  }
  
  test("Get message should return an empty JSON string if there is data, but none matches the Get criteria") {

    populateDataStore(dss, 3)
    val response3: Option[String] = dss !! (makeGet(List("A"), List("price"), thenms + 3000, thenms + 4000), 10000)
    response3.get.toString should equal ("{}")
  }

  test("Get message should return the one datum as a JSON string if there is one datum and it matches the Get criteria") {

    populateDataStore(dss, 1)
    val response2: Option[String] = dss !! (makeGet(List("A"), List("price"), epochStart, now), 10000)
    response2.get.toString should equal (makeJSONString(makeJSONRecord(thenms, "A", 0.0)))
  }
      
  test("Get message should return all data as a single JSON string if there is more than one datum and all match the Get criteria") {

    populateDataStore(dss, 3)
    val response3: Option[String] = dss !! (makeGet(List("A"), List("price"), epochStart, now), 10000)
    response3.get.toString should equal (makeJSONString(
      makeJSONRecord(thenms,        "A",  0.0),
      makeJSONRecord(thenms + 1000, "A", 10.0),
      makeJSONRecord(thenms + 2000, "A", 20.0)))
  }
}

