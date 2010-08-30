package org.chicagoscala.awse.domain.finance
import org.joda.time._

/**
 * Builder for constructing of a map of "criteria" with convenience methods for extraction, etc.
 * Note: I didn't use a case class, because our handling of unapply is nonstandard.
 */
class CriteriaMap(val map: Map[String, Any]) {
  import CriteriaMap._
  
  def withInstruments(instruments: String): CriteriaMap = withInstruments(Instrument.makeInstrumentsList(instruments))
  def withInstruments(instruments: Instrument*): CriteriaMap = withInstruments(instruments.toList)
  def withInstruments(instruments: List[Instrument]): CriteriaMap =
    new CriteriaMap(map + ("instruments" -> instruments))
    
  def withStatistics(statistics: String): CriteriaMap = withStatistics(InstrumentStatistic.makeInstrumentStatisticsList(statistics))
  def withStatistics(statistics: InstrumentStatistic*): CriteriaMap = withStatistics(statistics.toList)
  def withStatistics(statistics: List[InstrumentStatistic]): CriteriaMap =
    new CriteriaMap(map + ("statistics" -> statistics))

  def withStart(start: String): CriteriaMap = withStart(makeDateTime(start, (l:Long) => new DateTime(l)))
  def withStart(start: Long): CriteriaMap = withStart(new DateTime(start))
  def withStart(start: DateTime): CriteriaMap =
    new CriteriaMap(map + ("start" -> start))

  def withEnd(end: String): CriteriaMap = withEnd(makeDateTime(end, computeEndFromMillis _))
  def withEnd(end: Long): CriteriaMap = withEnd(new DateTime(end))
  def withEnd(end: DateTime): CriteriaMap =
    new CriteriaMap(map + ("end" -> end))
    
  def get(key: String) = map get key
  def getOrElse(key: String, default: => Any) = map.getOrElse (key, default)

  def instruments = getOrElse("instruments", List[Instrument]()).asInstanceOf[List[Instrument]]
  def statistics  = getOrElse("statistics",  List[InstrumentStatistic]()).asInstanceOf[List[InstrumentStatistic]]
  def start       = determineStart(this)
  def end         = determineEnd(this)
  
  override def toString = map.toString
  override def equals(that: Any): Boolean = that match {
    case cm: CriteriaMap => map equals cm.map
    case _ => false
  }
  override def hashCode: Int = map.hashCode
  
  /** Determine the start time from the criteria map, using a default value if necessary. */
  protected def determineStart(criteria: CriteriaMap): DateTime = criteria.get("start") match {
    case None => defaultStartTime
    case Some(x) => x match {
      case d: DateTime => d
      case l: Long => new DateTime(l)
      case _ => makeDateTime(x.toString, (l:Long) => new DateTime(l))
    }
  }

  /** 
   * Determine the end time from the criteria map, using a default value if necessary.
   */
  protected def determineEnd(criteria: CriteriaMap): DateTime = criteria.get("end") match {
    case None => defaultEndTime
    case Some(x) => x match {
      case d: DateTime => d
      case l: Long => computeEndFromMillis(l)
      case _ => makeDateTime(x.toString, computeEndFromMillis _)
    }
  }

  protected def computeEndFromMillis(millis: Long) = 
    if (millis > 0) new DateTime(millis) else defaultEndTime

  protected def makeDateTime(dateTimeString: String, makeFromMillis: Long => DateTime) = tryLong(dateTimeString) match {
    case Some(millis) => makeFromMillis(millis)
    case None => new DateTime(dateTimeString)
  }

  protected def tryLong(candidate: String): Option[Long] = try {
    Some(java.lang.Long.parseLong(candidate))
  } catch {
    case ex => None
  }
  
}

object CriteriaMap {
  
  def defaultStartTime = new DateTime(0)
  def defaultEndTime   = new DateTime
  
  implicit def apply(map: Map[String, Any]) = new CriteriaMap(map)
  implicit def apply() = new CriteriaMap(Map[String,Any]())
  
  type UnapplyType = Tuple4[List[Instrument], List[InstrumentStatistic], DateTime, DateTime]
  
  def unapply(criteria: CriteriaMap): Option[UnapplyType] = 
    Some(Tuple4(criteria.instruments, criteria.statistics, criteria.start, criteria.end))
}
