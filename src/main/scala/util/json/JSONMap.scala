package org.chicagoscala.awse.util.json
import net.lag.logging.Level                                   
import net.liftweb.json.Implicits
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser.parse
import org.bson.types.ObjectId

/**
 * Utility for converting between maps and map elements and the corresponding Lift JSON objects.
 * Implements a "type class" of sorts...
 * @note conversion from JSON objects to maps is provided by the <tt>JValue.values</tt> method.
 */
object JSONMap {
  
  class JSONStringizer(json: JValue) {
    def toJSONString = compact(render(json))
  }
  
  implicit def jsonToJSONStringizer(json: JValue) = new JSONStringizer(json)
  
  
  implicit def pairToJSON[A,B](p: Pair[A,B]): JField = JField(p._1.toString, toJValue(p._2))
  
  implicit def iterableToJSON[T](i: Iterable[T]): JArray  = JArray(i map (toJValue(_)) toList)  
  
  implicit def mapToJSON[K,V](m: Map[K,V]): JObject = JObject(m map (pairToJSON(_)) toList)
  
  implicit def toJValue(a: Any) = a match {
    case j: JValue       => j
    case m: Map[_,_]     => mapToJSON(m)
    case i: Iterable[_]  => iterableToJSON(i)
    case p: Pair[_,_]    => pairToJSON(p)
    case s: String       => stringToJValue(s)
    case l: Long         => JInt(BigInt(l))
    case i: Int          => JInt(BigInt(i))
    case bi:BigInt       => JInt(bi) //bi.longValue)
    case d: Double       => JDouble(d)
    case f: Float        => JDouble(f)
    case b: Boolean      => JBool(b)
    case id:ObjectId     => JString(id.toString)
    case a: AnyRef       => JString(a.toString)
  } 
   
   /**
    * Convert a JSON object to a Map. To avoid problems where longs get converted to
    * doubles by MongoDB, we convert any BigInts to longs. Also, we look for numeric
    * values in strings and return the value instead.
    */ 
   implicit def jsonToMap(json: JValue) = convertNumerics(json.values.asInstanceOf[Map[String,Any]])

   protected def convertNumerics(map: Map[String,Any]): Map[String,Any] = 
     map map { kv => (kv._1, convertNumerics(kv._2)) }

   protected def convertNumerics(i: Iterable[Any]): Iterable[Any] = 
     i map { convertNumerics(_) }

   protected def convertNumerics(x: Any): Any = x match {
     case bi: BigInt => bi.longValue
     case s:  String => stringToValue(s)
     case _ => x
   }

  /**
   * Convert quoted strings that are actually numeric values into the corresponding JSON numeric values.
   * For example, When Lift's JSON library converts XML to JSON, it simply transforms quoted attributes
   * to strings, even when they are actually numbers.
   * TODO: This is not invoked when Lift's parse method is used to parse a JSON string.
   * TODO: Eliminate duplication with stringToValue.
   */
  protected def stringToJValue(s: String) = (tryJInt(s) orElse tryJDouble(s) orElse Some(JString(s))).get
  
  protected def tryJDouble(s: String): Option[JValue] = try {
    val d = java.lang.Double.parseDouble(s)
    Some(JDouble(d))
  } catch {
    case th => None
  }

  protected def tryJInt(s: String): Option[JValue] = try {
    val l = java.lang.Long.parseLong(s)
    Some(JInt(l))
  } catch {
    case th => None
  }

  protected def stringToValue(s: String) = (tryLong(s) orElse tryDouble(s) orElse Some(s)).get
  
  protected def tryDouble(s: String): Option[Any] = try {
    val d = java.lang.Double.parseDouble(s)
    Some(d)
  } catch {
    case th => None
  }

  protected def tryLong(s: String): Option[JValue] = try {
    val l = java.lang.Long.parseLong(s)
    Some(l)
  } catch {
    case th => None
  }
}
