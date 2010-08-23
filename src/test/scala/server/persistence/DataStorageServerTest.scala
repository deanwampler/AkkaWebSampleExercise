package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.server._
import org.chicagoscala.awse.persistence.inmemory.InMemoryDataStore
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import org.joda.time.format._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

class DataStorageServerTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {
  
  val MAX = 100000
  val epochStart = new DateTime(0)
  val now        = new DateTime()
  val thenms     = now.getMillis - 3600
  
  def populateDataStore(server: ActorRef, numberOfItems:Int, offset: Int = 0) = {
    val data = for {
      i <- 0 until numberOfItems
      i2 = i + offset
      time = new DateTime(thenms + (1000L * i2))
      keyValue = "{\"value_"+i2+"\": "+(i2*i2)+"}"
    } yield (server !! Put(time, keyValue))
  }
  
  // Hacky: we combine what should be separate tests into one so we only have to worry
  // about setting up the server once, etc.
  test("Get message should return an JSON object") {
    val server = actorOf(new DataStorageServer("testService") {
      override lazy val dataStore = new InMemoryDataStore[String]("testDataStore")
    })
    server.start

    // if there is no data
    populateDataStore(server, 0)
    val response1: Option[String] = server !! (Get(epochStart, now), 10000)
    response1.toString should equal ("""Some([])""")

    // with 1 datum
    populateDataStore(server, 1)
    val response2: Option[String] = server !! (Get(epochStart, now), 10000)
    response2.toString should equal ("""Some([{"value_0": 0}])""")
    
    // with more than 1
    populateDataStore(server, 1, 1)
    val response: Option[String] = server !! (Get(epochStart, now), 10000)
    response.toString should equal ("""Some([{"value_0": 0}, {"value_1": 1}])""")

    server.stop
  }
}

