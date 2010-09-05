package org.chicagoscala.awse.server.rest
import org.chicagoscala.awse.util.json._
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.server.finance._
import org.chicagoscala.awse.domain.finance._
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor.ActorRef
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

class RestfulDataPublisherTest extends FunSuite 
    with ShouldMatchers with BeforeAndAfterEach {
  
  val now          = new DateTime
  val nowms        = now.getMillis
  val thenms       = nowms - 10000
  val epochStart   = new DateTime(0)
  val epochStartms = epochStart.getMillis

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
  
  def makeJSONString(json: JValue): String = compact(render(json))
  
  def makeJSON(list: List[JSONRecord]): JValue = list reduceLeft (_ ++ _) json

  def makeExpected(result: JValue): JValue = JSONMap toJValue Map("financial-data" -> result)
  
  var returnedJSON: JValue = _
  var ias: ActorRef = _
  var restfulPublisher: RestfulDataPublisher = _

  override def beforeEach = {
    restfulPublisher = new RestfulDataPublisher {
      override def instrumentAnalysisServerSupervisors = {
        val fake = actorOf(new Actor {
          def receive = {
            case CalculateStatistics(x) => self.reply(returnedJSON)
          }
        }) 
        fake.start
        List(fake)
      }
    }
  }
  
  test ("getAllDataFor returns a JSON string containing all data when all data matches the query criteria") {
    returnedJSON = makeJSON (List(js(0), js(3), js(2), js(4), js(1)))
    restfulPublisher.getAllDataFor("A,B,C","price", "0", nowms.toString) should equal (makeJSONString(makeExpected(returnedJSON)))
  }
  
  test ("getAllDataFor returns a JSON string containing all data that matches the time criteria") {
    // Return all data for the specified time range, low (inclusive) to high (exclusive)
    returnedJSON = makeJSON (List(js(3), js(2), js(4)))
    restfulPublisher.getAllDataFor("A,B,C" , "price" , (thenms + 1000).toString, (thenms + 3001).toString) should equal (
      makeJSONString(makeExpected(returnedJSON)))
  }
  
  test ("The time criteria are inclusive for the earliest time and exclusive for the latest time") {
    returnedJSON = makeJSON (List(js(3), js(2)))
    restfulPublisher.getAllDataFor("A,B,C" , "price" , (thenms + 1000).toString, (thenms + 3000).toString) should equal (
      makeJSONString(makeExpected(returnedJSON)))
  }
  
  // TODO
  test ("getAllDataFor returns a JSON string containing all data that matches the instrument criteria") {
    pending
  }
  
  // TODO
  test ("getAllDataFor returns a JSON string containing all data that matches the statistics criteria") {
    pending
  }
  
  test ("if one or both input times are invalid, getAllDataFor should return an error message") {
    restfulPublisher.getAllDataFor("A,B,C", "price", "x", "y") should equal (
      """{"error": "Invalid date time string: x. Investment instruments = 'A,B,C', statistics = 'price', start = 'x', end = 'y'."}""")
    restfulPublisher.getAllDataFor("A,B,C", "price", "2010-01-01", "y") should equal (
        """{"error": "Invalid date time string: y. Investment instruments = 'A,B,C', statistics = 'price', start = '2010-01-01', end = 'y'."}""")
  }
  
  test ("getAllDataFor should return an error message is there are appear to be no worker servers available") {
    val restfulPublisherWithNoDataStorageServers = new RestfulDataPublisher {
      override def instrumentAnalysisServerSupervisors = Nil
    }

    restfulPublisherWithNoDataStorageServers.getAllDataFor("A,B,C", "price", "0", "-1") should equal (
      """{"error": "No worker servers appear to be available!. Investment instruments = 'A,B,C', statistics = 'price', start = '0', end = '-1'."}""")
  }

}
