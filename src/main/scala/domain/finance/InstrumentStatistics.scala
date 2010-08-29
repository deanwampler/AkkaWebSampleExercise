package org.chicagoscala.awse.domain.finance
import se.scalablesolutions.akka.util.Logging
import org.chicagoscala.awse.util.error

/**
 * All the known statistics to calculate. However, this implementation
 * breaks the Expression problem.
 * TODO: Reimplement as type classes.
 */
sealed trait InstrumentStatistic
object InstrumentStatistic extends Logging {

  def make(whichKind: String) = whichKind match {
    case Price.NAME_RE(name) => Price
    case MovingAverage.NAME_RE(name, length) => MovingAverage(Integer.parseInt(length))
    case _ => error("Unrecognized statistic kind: [" + whichKind + "]")
  }
  
  def makeStatisticsList(names: String*): List[InstrumentStatistic] = 
    makeStatisticsList(names toList)

  def makeStatisticsList(names: List[String]): List[InstrumentStatistic] = 
    names flatMap { _.split("\\s*,\\s*") map make } toList
}

case object Price extends InstrumentStatistic {
  val NAME_RE = """^\s*([pP]rice)\s*$""".r
  
  override def toString = "price"
}
case class MovingAverage(length: Int) extends InstrumentStatistic {
  
  override def toString = length + "-day moving average"
}

object MovingAverage {
  /** Expected names, e.g., "50dma" or "50DMA" - 50 day moving average. */
  val NAME_RE = """^\s*((\d+)[dD][mM][aA])\s*$""".r
  
  val FIFTY_DAY       = MovingAverage(50) 
  val TWO_HUNDRED_DAY = MovingAverage(200) 
}
