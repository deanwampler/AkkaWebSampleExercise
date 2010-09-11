package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.persistence.inmemory._
import org.chicagoscala.awse.util.json.JSONMap._
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._

class MongoDBJSONRecordTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {
  
  val now     = (new DateTime).getMillis
  val data    = List(List(1L, 1.1), List(2L, 2.2))
  val map     = Map("timestamp" -> BigInt(now), "data" -> data)
  val recordJSON = 
      JSONRecord(("timestamp" -> BigInt(now)) ~ 
      ("data" -> JArray(List(JArray(List(JInt(1L),JDouble(1.1))), JArray(List(JInt(2L),JDouble(2.2)))))))
  val recordMap = JSONRecord(map)
  
  var dataStore: InMemoryDataStore = _

  override def beforeEach = {
    dataStore = new InMemoryDataStore("testColl_testDb")
  }
  
  test ("JSONRecords written to a data store and retrieved have valid timestamps") {
    dataStore add recordJSON
    val results = dataStore.getAll.toList
    results.size should equal (1)
    (results(0).json \ "timestamp") should equal(recordJSON.json \ "timestamp")
    (results(0).json \ "timestamp") should equal(JField("timestamp", JInt(now)))
  }
  
  test ("JSONRecords constructed with JSON and written to a data store with embedded Long values should still those values as Longs when retrieved") {
    dataStore add recordJSON
    val results = dataStore.getAll.toList
    val resultsData = (results(0).json \ "data").values
    val expectedData = Pair("data", data)
    resultsData should equal(expectedData)
    (results(0).json \ "data").toJSONString should equal("\"data\":[[1,1.1],[2,2.2]]")
  }
  
  test ("JSONRecords constructed with Maps and written to a data store with embedded Long values should still those values as Longs when retrieved") {
    dataStore add recordMap
    val results = dataStore.getAll.toList
    val resultsData = (results(0).json \ "data").values
    val expectedData = Pair("data", data)
    resultsData should equal(expectedData)
    // TODO: Should not return floats for the 1st elements in the pairs in the data array!
    (results(0).json \ "data").toJSONString should equal("\"data\":[[1.0,1.1],[2.0,2.2]]")
  }
  
  test ("JSONRecords constructed with JSON and written to a data store with embedded Double values should still those values as Doubles when retrieved") {
    dataStore add recordJSON
    val results = dataStore.getAll.toList
    val resultsData = (results(0).json \ "data").values
    val expectedData = Pair("data", data)
    resultsData should equal(expectedData)
    (results(0).json \ "data").toJSONString should equal("\"data\":[[1,1.1],[2,2.2]]")
  }
  
  test("++(JSONRecord) returns a new JSONRecord with the combination of the original record's JSON and the argument JSON") {
    val recordJSONWithId = recordJSON ++ JSONRecord(Map("_id" -> "foo", "timestamp" -> 0L))
    recordJSONWithId.json should equal (recordJSON.json ++ (("_id" -> "foo") ~ ("timestamp" -> 0L)))
  }
  test("++(JValue) returns a new JSONRecord with the combination of the original record's JSON and the argument JSON") {
    val recordJSONWithId = recordJSON ++ ("_id" -> "foo")
    recordJSONWithId.json should equal (recordJSON.json ++ ("_id" -> "foo"))
  }
  
  test("merge(JSONRecord) returns a new JSONRecord with the combination of the original record's JSON and the argument JSON") {
    val recordJSONWithId = recordJSON merge JSONRecord(Map("_id" -> "foo", "timestamp" -> 0L))
    recordJSONWithId.json should equal (recordJSON.json merge (("_id" -> "foo") ~ ("timestamp" -> 0L)))
  }
  test("merge(JValue) returns a new JSONRecord with the combination of the original record's JSON and the argument JSON") {
    val recordJSONWithId = recordJSON merge ("_id" -> "foo")
    recordJSONWithId.json should equal (recordJSON.json merge ("_id" -> "foo"))
  }
  
  test("toMap converts the record to a Map") {
    recordJSON.toMap should equal(map)
  }
  
  test("JSONRecord.equalsIgnoringId should return true if two records are equal ignoring their ids") {
    val recordJSONWithId = recordJSON merge ("_id" -> "foo")
    recordJSONWithId equalsIgnoringId recordJSON should equal (true)
  }

  test("JSONRecord.apply can be constructed with a JValue") {
    recordJSON.toMap should equal(recordMap.toMap)
  }
  
  test("JSONRecord.jsonWithoutId applied to a JSONRecord(JObject) returns the record's JSON without an \"_id\" field, if present") {
    val recordJSONWithId = recordJSON merge ("_id" -> "foo")
    JSONRecord.jsonWithoutId(recordJSONWithId) should equal (recordJSON.json)
    JSONRecord.jsonWithoutId(recordJSON)       should equal (recordJSON.json)
  }
  
  test("JSONRecord.jsonWithoutId applied to a JSONRecord(JArray) returns the array's JSON without an \"_id\" field, if present") {
    val jf = JField("_id2", JInt(2))
    val arrayWith    = JArray(List(JField("_id", JInt(1)), jf, JField("timestamp", JInt(now))))
    val arrayWithOut = JArray(List(jf, JField("timestamp", now)))
    JSONRecord.jsonWithoutId(JSONRecord(arrayWith))    should equal (arrayWithOut)
    JSONRecord.jsonWithoutId(JSONRecord(arrayWithOut)) should equal (arrayWithOut)
  }
  
  test("JSONRecord.jsonWithoutId applied to a JSONRecord(JField(\"timestamp\", ...)), i.e., without the \"_id\" key, returns the same JField") {
    val jf = JField("timestamp", JInt(now))
    JSONRecord.jsonWithoutId(JSONRecord(jf)) should equal (jf)
  }
}
