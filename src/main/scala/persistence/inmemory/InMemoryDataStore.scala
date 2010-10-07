package org.chicagoscala.awse.persistence.inmemory
import org.chicagoscala.awse.persistence._
import se.scalablesolutions.akka.util.Logging
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
  
  def range(from: DateTime, to: DateTime, otherCriteria: JValue, maxNum: Int): Iterable[JSONRecord] = 
    store.range(from, dateTimePlus1(to)).map(p => p._2).take(maxNum).toIterable

  /**
   * A limited query capability. Currently only supports the same operations as range, specified thusly:
   * Map(">=" -> lowerDateTime, "<=" -> upperDateTime, "max" => maxNum). Other key-value pairs are ignored.
   */
  // def query(querySpecification: Map[String, Any]): Iterable[JSONRecord] = {
  //   val lower = querySpecification.getOrElse(">=",  new DateTime(0)).asInstanceOf[DateTime]
  //   val upper = querySpecification.getOrElse("<=",  new DateTime).asInstanceOf[DateTime]
  //   val max   = querySpecification.getOrElse("max", java.lang.Integer.MAX_VALUE).asInstanceOf[Int]
  //   range(lower, upper, max)
  // }

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

