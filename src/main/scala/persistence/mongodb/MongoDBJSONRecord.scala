package org.chicagoscala.awse.persistence.mongodb
import org.chicagoscala.awse.persistence._
import com.osinka.mongodb._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import com.mongodb._

/**
 * A Utility that handles conversion between MongoDB JSON objects and JSONRecords.
 * @note I attempted to use scala-mongo-driver's protocol for wrapping DBObject, 
 * but it seems to only work for fixed "schema" objects, not arbitrary maps, which
 * we prefer to use (TODO - fix?).
 */
object MongoDBJSONRecord {
  
  /**
   * Convert a Map to DBObject.
   */ 
  implicit def toDBObject(json: JSONRecord) = mapToDBObject(json.toMap)
  
  def apply(dbo: DBObject): JSONRecord = JSONRecord(dbo.toMap.asInstanceOf[java.util.Map[String,Any]])
      
  def mapToDBObject(map: Map[String,Any]): DBObject = {
  	val dbo = new BasicDBObject
  	map map { kv => kv._2 match {
  	  case map2: Map[_,_] => dbo.put(kv._1, mapToDBObject(map2.asInstanceOf[Map[String,Any]]))
  		case iter:Iterable[_] => dbo.put(kv._1, iterToDBObject(iter))
  		case _ => dbo.put(kv._1, otherToDBValue(kv._2))
  	}}
  	dbo
  }

  def iterToDBObject(iter: Iterable[Any]): DBObject = {
  	val dbl = new BasicDBList
  	iter map {
  	  case map: Map[_,_] => dbl.add(mapToDBObject(map.asInstanceOf[Map[String,Any]]))
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
