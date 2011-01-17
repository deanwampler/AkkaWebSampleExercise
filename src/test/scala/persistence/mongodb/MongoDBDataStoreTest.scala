package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._

/**
 * Mocks Mongo by using an in-memory DB instead when doing CI builds.
 */
class MongoDBDataStoreTest extends DataStoreTestBase with BeforeAndAfterEach with BeforeAndAfterAll {

  type DS = MongoDBDataStore
  var dataStore: DS = _
  
  override def beforeAll = {
    dataStore = new MongoDBDataStore("testColl", "testDb")(dateTime => dateTime.getMillis)
  }
  
  override def afterEach = dataStore.collection.drop

  override def afterAll = dataStore.dataBase.dropDatabase
  
  
}
