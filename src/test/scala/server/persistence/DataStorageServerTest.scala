package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
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
  

  def makeJSONRecord(time: Long, value: String) = 
    JSONRecord(("timestamp" -> time) ~ ("value" -> value))
  
  def makeJSON(list: JSONRecord*) = (list reduceLeft (_ ++ _)) json

  def makeGet(start: Long, end: Long) = Get(("start" -> start) ~ ("end" -> end))
  
  def sendAndWait(msg: Message): Option[String] = {
    answer = (driverActor !!! msg).await.result
    answer
  }

  def populateDataStore(numberOfItems:Int) = {
    val data = for {
      i <- 0 until numberOfItems
      time = thenms + (1000L * i)
      json = makeJSONRecord(time, "value: "+i)
    } yield sendAndWait(Put(json))
    testDataStore.size should equal (numberOfItems)
  }

  var testDataStore: InMemoryDataStore = _
  var dss: ActorRef = _
  var driverActor: ActorRef = _
  var answer: Option[String] = None
  
  override def beforeEach = {
    testDataStore = new InMemoryDataStore("testDataStore")
    dss = actorOf(new DataStorageServer("testService", testDataStore))
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
  }
  override def afterEach = {
    dss.stop
    driverActor.stop
  }
  
  test("Get message should return an empty JSON string if there is no data") {

    val response1: Option[_] = sendAndWait(makeGet(epochStart, now))
    response1.get should equal (JNothing)
  }
  
  test("Get message should return an empty JSON string if there is data, but none matches the Get time-range criteria") {

    populateDataStore(3)
    val response3: Option[_] = sendAndWait(makeGet(thenms + 3000, thenms + 4000))
    response3.get should equal (JNothing)
  }

  test("Get message should return the one datum as a JSON string if there is one datum and it matches the Get time-range criteria") {

    populateDataStore(1)
    val response2: Option[_] = sendAndWait(makeGet(epochStart, now))
    response2.get should equal (makeJSONRecord(thenms, "value: 0").json)
  }
      
  test("Get message should return all data as a single JSON string if there is more than one datum and all match the Get time-range criteria") {

    populateDataStore(3)
    val response3: Option[_] = sendAndWait(makeGet(epochStart, now))
    response3 match {
      case None => fail()
      case Some(s) => 
        s should equal (makeJSON(
          makeJSONRecord(thenms,        "value: 0"),
          makeJSONRecord(thenms + 1000, "value: 1"),
          makeJSONRecord(thenms + 2000, "value: 2")))
    }
  }

  test("Get message should return data as a single JSON string if there is data that matches the Get value criteria") {
    pending  // TODO: implement this functionality!
  }
}

