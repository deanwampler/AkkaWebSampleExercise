package org.chicagoscala.awse.domain.finance
import org.chicagoscala.awse.util.json.JSONMap._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser.parse

/**
 * "Type classes" to convert the finance-specific types to Lift JSON objects.
 * TODO: Add conversions for JSON -> Instrument, etc.
 */
object FinanceJSONConverter {
  
  /**
   * Convert an instrument to JSON.
   * Note that return a Map, but a JValue is required - the implicit conversions
   * imported from JSONMap convert the Map to a JValue.
   */
  implicit def instrumentToJSON(instrument: Instrument): JValue = 
    Map("instrument" -> Map("symbol" -> instrument.symbol))
  
    /**
     * Convert an instrument to JSON.
     */
  implicit def statisticToJSON(statistic: InstrumentStatistic): JValue = statistic match {
    case Price(currency) => 
      Map("price" -> Map("currency" -> currency.toString))
    case Dividend(currency) => 
      Map("dividend" -> Map("currency" -> currency.toString))
    case MovingAverage(length) => 
      Map("moving average" -> Map("length" -> length))
  }
}
