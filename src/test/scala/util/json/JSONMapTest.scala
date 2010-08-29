package org.chicagoscala.awse.util.json
import org.chicagoscala.awse.util.json.JSONMap._

import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers

import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonParser.parse

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

  test("The implicit mapToJSON should convert a Map to a JSON Map") {
    val js: JValue = bigMap
    js.toJSONString should equal (bigMapStr)
  }

  test("The implicit iterableToJSON should convert a Seq to a JSON array") {
    val list = List(List(1L, "one"), List(2L, "two"))
    val js: JValue = list
    js.toJSONString should equal ("""[[1,"one"],[2,"two"]]""")
  }

  test("The implicit pairToJSON should convert a Pair to a JSON key:value") {
    val pair = Pair("1", "one")
    val js: JValue = pair
    js.toJSONString should equal ("\"1\":\"one\"")
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
