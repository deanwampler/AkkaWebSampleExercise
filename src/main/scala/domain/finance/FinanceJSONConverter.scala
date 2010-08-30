package org.chicagoscala.awse.domain.finance
import org.chicagoscala.awse.util.json.JSONMap._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonParser.parse

/**
 * "Type classes" to conver the finance-specific types to Lift JSON objects.
 * TODO: Add conversions for JSON -> Instrument, etc.
 */
object FinanceJSONConverter {
  
  implicit def instrumentToJSON(instrument: Instrument): JValue = 
    Map("instrument" -> Map("symbol" -> instrument.symbol))
  
  implicit def statisticToJSON(statistic: InstrumentStatistic): JValue = statistic match {
    case Price(currency) => 
      Map("price" -> Map("currency" -> currency.toString))
    case MovingAverage(length) => 
      Map("moving average" -> Map("length" -> length))
  }
}
