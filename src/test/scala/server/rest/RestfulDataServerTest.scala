package org.chicagoscala.awse.server.rest
import org.chicagoscala.awse.server.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.scalatest.{FlatSpec, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import scala.collection.mutable.{Map => MMap}

class RestfulDataServerTest extends FunSuite with ShouldMatchers {
  
  val now          = new DateTime
  val nowms        = now.getMillis
  val epochStart   = new DateTime(0)
  val epochStartms = epochStart.getMillis

  val dss = new DataStorageServer("testDataStorageService1") {
    override lazy val dataStore = new InMemoryDataStore[String]("testDataStore1")
  }  
  val restfulServer = new RestfulDataServer {
    override def dataStorageServers = List(dss)
  }
  
  val restfulServerWithNoDataStorageServers = new RestfulDataServer {
    override def dataStorageServers = Nil
  }
  
  def loadData {
    dss !! Put(new DateTime(nowms - 4000), """{"value4": 4}""")
    dss !! Put(new DateTime(nowms - 3000), """{"value3": 3}""")
    dss !! Put(new DateTime(nowms - 2000), """{"value2": 2}""")
    dss !! Put(new DateTime(nowms - 1000), """{"value1": 1}""")
    dss !! Put(now,                        """{"value0": 0}""")
  }
  
  // Hacky: we combine what should be separate tests into one so we only have to worry
  // about setting up the server once, etc.
  test ("getAllDataFor returns JSON based on the query parameters") {
    dss.start
    restfulServer.start
    loadData
    
    // Return all data
    restfulServer.getAllDataFor("primes",0,-1) should equal (
      """[[{"value4": 4}, {"value3": 3}, {"value2": 2}, {"value1": 1}, {"value0": 0}]]""")

    // Return all data for the specified range
    restfulServer.getAllDataFor("primes", nowms - 3000, nowms - 1000) should equal (
      """[[{"value3": 3}, {"value2": 2}]]""")

    dss.stop
    restfulServer.stop
  }
  
  test ("getAllDataFor should return an error message is there are appear to be no data servers available") {
    restfulServerWithNoDataStorageServers.start
    restfulServerWithNoDataStorageServers.getAllDataFor("primes", 100, 200) should equal (
      """{"error": "No data servers appear to be available."}""")
    restfulServerWithNoDataStorageServers.stop
  }
  
}
