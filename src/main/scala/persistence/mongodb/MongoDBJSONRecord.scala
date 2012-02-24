package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.util.json._
import org.chicagoscala.awse.persistence._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala._

/**
 * A Utility that handles conversion between MongoDB JSON objects and JSONRecords.
 */
object MongoDBJSONRecord {

	RegisterJodaTimeConversionHelpers()

  /**
   * Convert a JSONRecord to a DBObject.
   */ 
  implicit def toDBObject(json: JSONRecord): DBObject = (json.toMap)
  //implicit def toDBObject(json: JSONRecord): DBObject = mapToDBObject(json.toMap)
  
  /**
   * Convert a Lift JSON JValue to a DBObject.
   */ 
  implicit def toDBObject(json: JValue): DBObject = JSONMap.jsonToMap(json)

  def apply(dbo: DBObject): JSONRecord = JSONRecord(dbo.toMap.asInstanceOf[java.util.Map[String,Any]])
      
  def iterToDBObject(iter: Iterable[Any]): DBObject = {
  	val dbl = new BasicDBList
  	iter map {
  	  case map: Map[_,_] => dbl.add(map.asInstanceOf[Map[String,Any]])
  	  //case map: Map[_,_] => dbl.add(mapToDBObject(map.asInstanceOf[Map[String,Any]]))
  		case iter2:Iterable[_] => dbl.add(iterToDBObject(iter2))
  		case x => dbl.add(otherToDBValue(x))
  	}
  	dbl
  }
  
  def otherToDBValue(x: Any): AnyRef = x match {
    case bi: BigInt  => new java.lang.Long(bi.longValue)
    case ar: AnyRef  => ar
    case d:  Double  => new java.lang.Double(d)
    case f:  Float   => new java.lang.Float(f)
    case l:  Long    => new java.lang.Long(l)
    case i:  Int     => new java.lang.Integer(i)
    case s:  Short   => new java.lang.Short(s)
    case b:  Byte    => new java.lang.Byte(b)
    case c:  Char    => new java.lang.Character(c)
    case b:  Boolean => new java.lang.Boolean(b)
    case u:  Unit    => "()"
  }
}
