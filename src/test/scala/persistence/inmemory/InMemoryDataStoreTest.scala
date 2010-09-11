package org.chicagoscala.awse.persistence.inmemory
import org.chicagoscala.awse.persistence._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._

class InMemoryDataStoreTest extends DataStoreTestBase with BeforeAndAfterEach {

  type DS = InMemoryDataStore
  var dataStore: DS = _
  
  override def beforeEach {
    dataStore = new InMemoryDataStore("testStore")
  }
}
