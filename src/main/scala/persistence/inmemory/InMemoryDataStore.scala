package org.chicagoscala.awse.persistence.inmemory
import org.chicagoscala.awse.persistence._
import akka.util.Logging
import scala.collection.SortedMap
import org.joda.time._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

/**
 * Pure, in-memory storage of data with no backing store. Note that it may not perform well
 * as the collection grows large.
 * This is largely used only for testing. The MongoDBDataStore provides the features we really
 * need for the "production" app.
 */
class InMemoryDataStore(val name: String) extends DataStore with Logging {

  implicit val dateTimeOrdering = new scala.math.Ordering[DateTime] {
    def compare(dt1: DateTime, dt2: DateTime) = dt1 compareTo dt2
  }
  
  var store = SortedMap[DateTime, JSONRecord]()
  
  def add(item: JSONRecord): Unit = store += Pair(item.timestamp, item)
    
  def getAll() = store map {p => p._2}
  
  def range(from: DateTime, to: DateTime, otherCriteria: Map[String,Any] = Map.empty, maxNum: Int): Iterable[JSONRecord] = {
    // Get the data as as a map of times to JSONRecords in the time range and extract just the JSONRecords and convert them to Maps.
    val rangeResult = store.range(from, dateTimePlus1(to)).toIterable map { p => p._2.toMap }
    val rangeResult2: Iterable[Map[String,Any]] = otherCriteria.size match {
      case 0 => rangeResult
      case _ => rangeResult filter filters(otherCriteria)
    } 
    rangeResult2 map { (r: Map[String,Any]) => JSONRecord(r) } take maxNum
  }
  
  // A function value that takes a criteria map and returns another function that takes a map of data. 
  // It requires the data map to satisfy at least one of the criteria.
  val filters = (criteria: Map[String,Any]) => (data: Map[String,Any]) => 
    criteria exists { kv => 
      data.get(kv._1) match {
        case None => false
        case Some(value) => kv._2 match {
          // If the criterium is a list or array, require that the data value (in the key-value) is 
          // present in the list. In other words, the criterium list elements are treated as "or'ed" 
          // requirements.
          // If the criterium is a map, we don't support it yet.
          // For anything else, we require the data value to equal the criterium value.
          case list: List[_]  => list contains value
          case map:  Map[_,_] => throw new RuntimeException("Construction of Query with a Map is TODO.")
          case a:    Any      => a == value
        }
      }
    }    
  
  def getDistinctValuesFor(keyForValues: String) = {
    val values = store.valuesIterator map { (jsonRecord: JSONRecord) =>
      (jsonRecord.json \ keyForValues) match {
        case JField(_, JString(s)) => s
        case x => throw new RuntimeException("Value '" + x + "' for '"+ keyForValues + "' is not a string. json = "+jsonRecord)
      }
    }
    println ("values: "+values)
    List(JSONRecord(keyForValues -> values.toSet.toList.sortWith(_.compareTo(_) < 0)))
  }

  def size: Long = store.size
  
  protected def dateTimePlus1(dt: DateTime) = new DateTime(dt.getMillis+1)
}

