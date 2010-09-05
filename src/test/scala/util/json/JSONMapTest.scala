package org.chicagoscala.awse.util.json
import org.chicagoscala.awse.util.json.JSONMap._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser.parse
import org.bson.types.ObjectId
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers


class JSONMapTest extends FunSuite with ShouldMatchers {
  val bigMapStr = """{"first":"firstValue","second":["second1",2,2.2],"third":{"three":3,"thirty three":33},"fourth":[[1,"one"],[2,"two"]]}"""
  val bigMap = Map(
    "first" -> "firstValue",
    "second" -> List("second1", 2L, 2.2),
    "third" -> Map(
      "three" -> 3L,
      "thirty three" -> 33L),
    "fourth" -> List(List(1L, "one"), List(2L, "two"))
  )
  val bigMapJSON: JValue = bigMap
  
  val longMap = Map("big" -> BigInt(100L), "long" -> "1000", "double" -> "1.1")
  val numberStringMap = Map("long" -> "100", "double" -> "1.1")

  test("toJSONString creates a JSON string from a JValue") {
    bigMapJSON.toJSONString should equal (bigMapStr)   
  }

  test("The implicit mapToJSON should convert a Map to a JObject (map)") {
    val js: JValue = bigMap
    js.toJSONString should equal (bigMapStr)
  }

  test("The implicit iterableToJSON should convert a Seq to a JArray") {
    val list = List(List(1L, "one"), List(2L, "two"))
    val js: JValue = list
    js.toJSONString should equal ("""[[1,"one"],[2,"two"]]""")
  }

  // TODO: pairToJSON isn't invoked; another implicit method is invoked that turns it into
  // a JObject. Investigate...
  test("The implicit pairToJSON should convert a Pair to a JField") {
    pending
    val pair = Pair("1", "one")
    val js: JValue = pair
    js should equal (JField("1", "one"))
  }

  test("The implicit toJValue should return an input JValue unchanged") {
    toJValue(bigMapJSON) should equal (bigMapJSON)
  }
  
  test("The implicit toJValue should convert a Map to a JObject (map)") {
    val jv: JValue = toJValue(Map("a" -> "A", "one" -> 2))
    jv should equal (("a" -> "A") ~ ("one" -> 2))
  }

  test("The implicit toJValue should convert an Iterable to a JArray") {
    val list = List(Pair("1", "one"), Pair("2", "two"))
    val jv: JValue = toJValue(list)
    jv should equal (JArray(List(JField("1", JString("one")), JField("2", JString("two")))))
  }
  
  test("The implicit toJValue should convert a Pair to a JField") {
    val pair = Pair("1", "one")
    val jv: JValue = toJValue(pair)
    jv should equal (JField("1", JString("one")))
  }
  
  test("The implicit toJValue should convert a String to a JString") {
    val jv: JValue = toJValue("string")
    jv should equal (JString("string"))
  }
  
  test("The implicit toJValue should convert a Long, Int, or BigInt to a JInt") {
    val jv: JValue = toJValue(1)
    jv should equal (JInt(1))
    val jv2: JValue = toJValue(2L)
    jv2 should equal (JInt(2L))
    val jv3: JValue = toJValue(BigInt(3L))
    jv3 should equal (JInt(3L))
  }
  
  test("The implicit toJValue should convert a Double or Float to a JDouble") {
    val jv: JValue = toJValue(1.0)
    jv should equal (JDouble(1.0))
    val jv2: JValue = toJValue(2.0F)
    jv2 should equal (JDouble(2.0F))
  }

  test("The implicit toJValue should convert a Boolean to a JBoolean") {
    val jv: JValue = toJValue(true)
    jv should equal (JBool(true))
    val jv2: JValue = toJValue(false)
    jv2 should equal (JBool(false))
  }
  
  test("The implicit toJValue should convert a MongoDB ObjectId to a JString") {
    val jv: JValue = toJValue(new ObjectId("4c505d73d14b7b2270148fe3"))
    jv should equal (JString("4c505d73d14b7b2270148fe3"))
  }
  
  test("The implicit toJValue should convert any other AnyRef to a JString") {
    val jv: JValue = toJValue(Some("thing"))
    jv should equal (JString("Some(thing)"))
  }  

  test("When converting to JSON, quoted numeric values should be converted to their values") {
    val js:JValue = numberStringMap
    js.toJSONString should equal ("""{"long":100,"double":1.1}""")
  }
  
  test("The implicit jsonToMap should convert a JSON object to a Map") {
    mapToJSON(bigMap).toMap should equal (bigMap)
  }

  test("The implicit jsonToMap should convert BigInts in a JSON object to longs in the corresponding Map") {
    mapToJSON(longMap).toMap should equal (Map("big" -> 100L, "long" -> 1000L, "double" -> 1.1))
  }

  test("The implicit jsonToMap should convert numeric strings into their numeric values") {
    mapToJSON(longMap).toMap should equal (Map("big" -> 100L, "long" -> 1000L, "double" -> 1.1))
  }
}
