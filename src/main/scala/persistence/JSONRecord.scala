package org.chicagoscala.awse.persistence
import org.chicagoscala.awse.util.json.JSONMap._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser.parse
import org.joda.time._

/**
 * A wrapper around a Lift JSON object.
 */
case class JSONRecord(json: JValue) {
  
  val timestamp: DateTime = determineTimestamp(json)

  /**
   * Convert the JSON object to a Map. To avoid problems where longs get converted to
   * doubles by Mongo, we convert any BigInts to longs.
   */ 
  def toMap = convertBigIntsToLongs(json.values.asInstanceOf[Map[String,Any]])
  
  def ++(other: JSONRecord): JSONRecord = JSONRecord(json ++ other.json)
  def ++(other: JValue): JSONRecord     = JSONRecord(json ++ other)
    
  /** Merge two records. The "other's" JSON overwrites matching keys in this JSON. */
  def merge(other: JSONRecord): JSONRecord = this merge other.json
  def merge(other: JValue): JSONRecord     = JSONRecord(json merge other)

  override def toString = json.toString
  def toJSONString = compact(render(json))
  
  /**
   * Compare to records, ignoring any "_id" values. This is a big hack. What we would like
   * to do is remove the _id from each, then compare the resulting JSONs, but there doesn't 
   * appear to be a convenient way to do this.
   */
  def equalsIgnoringId(that: JSONRecord) = 
    JSONRecord.jsonWithoutId(this) equals JSONRecord.jsonWithoutId(that)
    
  protected def convertBigIntsToLongs(m: Map[String, Any]): Map[String, Any] = 
    m map { kv => (kv._1, convertBigIntsToLongs(kv._2)) }
    
  protected def convertBigIntsToLongs(i: Iterable[Any]): Iterable[Any] = 
    i map { convertBigIntsToLongs(_) }
    
  protected def convertBigIntsToLongs(x: Any): Any = x match {
    case bi: BigInt => bi.longValue
    case _ => x
  }

  protected def determineTimestamp(json: JValue): DateTime = 
    (for { 
      JField(key, timestamp) <- json
      if key == JSONRecord.timestampKey
    } yield jValueToTimestamp(timestamp)) toList match {
      case Nil => throw new JSONRecord.InvalidJSONException(json)
      case head :: _ => head
    }
  
  def jValueToTimestamp(jv: JValue): DateTime = try {
    jv match {
      case JInt(value) => new DateTime(value.toLong)
      case JString(value) => new DateTime(value)
      case _ => throw new JSONRecord.UnsupportedTimestampTypeException(jv)
    }
  } catch {
    case th: JSONRecord.UnsupportedTimestampTypeException => throw th
    case th => throw new JSONRecord.InvalidJSONException(jv, th)
  }
}

object JSONRecord {

  case class InvalidJSONException(json: JValue, cause: Throwable) extends RuntimeException(
    "JSON must have a valid " + timestampKey + " key-value: " + compact(render(json)), cause) {
    
    def this(json: JValue) = this(json, null)
  }
  
  case class UnsupportedTimestampTypeException(json: JValue, cause: Throwable) extends RuntimeException(
    "The timestamp value " + compact(render(json)) + " isn't one that is supported, Long or String", cause) {
    
    def this(json: JValue) = this(json, null)
  }

  // Should these values really be public? In a widely used public API, probably not, 
  // but in our restricted usage, the overhead of tighter encapsulation isn't really that valuable.
  val defaultTimestampKey = "timestamp"
  var timestampKey = defaultTimestampKey

  // Already supplied by the case keyword:
  // def apply(json: JValue): JSONRecord 
  def apply[K,V](map: Map[K,V]): JSONRecord = new JSONRecord(map)
  def apply[K,V](jmap: java.util.Map[K,V]): JSONRecord = new JSONRecord(javaMapToMap(jmap))  
  
  def jsonWithoutId(record: JSONRecord) = record.json match {
    case jo: JObject => JObject(jo.obj filter { 
      case JField("_id", value) => false
      case _ => true 
    })
    case ja: JArray => JArray(ja.arr filter { 
      case JField("_id", value) => false
      case _ => true 
    })
    case JField("_id", value) => JNothing
    case json => json 
  }
  
  // Since the underlying Java map is mutable, we make a copy, rather than use
  // Scala's JavaConversions.
  def javaMapToMap[K,V](jmap: java.util.Map[K,V]): Map[String,Any] =
    scala.collection.JavaConversions.asMap(jmap).foldLeft(Map[String,Any]()) { (map, kv) => 
      map + (kv._1.toString -> javaObjectToAny(kv._2)) 
    }
  
  def javaIterableToIterable(jiter: java.lang.Iterable[_]): Iterable[Any] = 
    scala.collection.JavaConversions.asIterable(jiter) map { x => javaObjectToAny(x) }

  def javaObjectToAny(jobj: Any): Any = jobj match {
    case l: java.lang.Long    => l.longValue
    case i: java.lang.Integer => i.intValue
    case d: java.lang.Double  => d.doubleValue
    case f: java.lang.Float   => f.floatValue
    case jmap2: java.util.Map[_,_] => javaMapToMap(jmap2)
    case jlist: java.lang.Iterable[_] => javaIterableToIterable(jlist)
    case _ => jobj
  }
}
