package org.chicagoscala.awse.domain.finance
import se.scalablesolutions.akka.util.Logging
import org.chicagoscala.awse.util.error

/**
 * The sealed traits here break the "Expression Problem"; you would like to add 
 * new currencies and statistics without touching this file.
 * TODO: Reimplement as type classes.
 */

/**
 * Currencies
 */
sealed trait Currency {
  def symbol: String
}
object Currency {
  // Treat "" and null as defaults for $. Null can arise because of 2nd capture group in Price.NAME_RE.
  def apply(s: String): Currency = s match {
    case "$" | "Dollars" | "dollars" | "" | null => Dollars
    case _ => throw UnknownCurrency(s)
  }
}

case object Dollars extends Currency {
  def symbol = "$"
  override def toString = symbol
}

case class UnknownCurrency(s: String) extends RuntimeException("Unknown currency: "+s)

/**
 * The possible statistics to calculate.
 */
sealed trait InstrumentStatistic
object InstrumentStatistic extends Logging {

  def make(whichKind: String): InstrumentStatistic = apply(whichKind)
  
  def apply(whichKind: String) = whichKind match {
    case Price.NAME_RE(name, currency) => Price(Currency(currency))
    case MovingAverage.NAME_RE(name, length) => MovingAverage(Integer.parseInt(length))
    case _ => error("Unrecognized type of statistic: [" + whichKind + "]")
  }
  
  def makeInstrumentStatisticsList(names: String*): List[InstrumentStatistic] = 
    makeInstrumentStatisticsList(names toList)

  def makeInstrumentStatisticsList(names: List[String]): List[InstrumentStatistic] = 
    names flatMap { _.split("\\s*,\\s*") map make } toList
}

case class Price(currency: Currency = Dollars) extends InstrumentStatistic {
  
  override def toString = "price[" + currency + "]"
}
object Price {
  // The 2nd (optional) capture group is the name or symbol of the currency.
  // (Note there is a noncapturing group outside the 2nd group.)
  val NAME_RE = """^\s*([pP]rice(?:\[([^\]]+)\])?)\s*$""".r
}

case class Dividend(currency: Currency = Dollars) extends InstrumentStatistic {
  
  override def toString = "dividend[" + currency + "]"
}
object Dividend {
  // The 2nd (optional) capture group is the name or symbol of the currency.
  // (Note there is a noncapturing group outside the 2nd group.)
  val NAME_RE = """^\s*([dD]ividend(?:\[([^\]]+)\])?)\s*$""".r
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
