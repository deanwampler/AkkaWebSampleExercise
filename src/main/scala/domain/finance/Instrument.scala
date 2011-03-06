package org.chicagoscala.awse.domain.finance

/**
 * A financial instrument.
 */
case class Instrument(symbol: String) 
object Instrument {  
  
  def makeInstrumentsList(names: String*): List[Instrument] = makeInstrumentsList(names toList)
  
  def makeInstrumentsList(names: List[String]): List[Instrument] = names flatMap { name =>
    name.split("\\s*,\\s*") map { n => Instrument(n.trim) } 
  } toList
  
  def toSymbolNames(instruments: List[Instrument]): List[String] = instruments map (_.symbol) toList
}
