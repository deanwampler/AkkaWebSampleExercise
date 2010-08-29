package org.chicagoscala.awse.domain.finance
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers

class InstrumentTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {
  def makeInstruments(names: String*): List[Instrument] = names map Instrument.apply toList
  
  test ("Instrument.makeInstrumentsList should accept a vararg list of strings") {
    Instrument.makeInstrumentsList("a,b", "c,d") should equal(makeInstruments("a", "b", "c", "d"))
  }

  test ("Instrument.makeInstrumentsList should accept split each string on commas") {
    Instrument.makeInstrumentsList("a,b", "c,d") should equal(makeInstruments("a", "b", "c", "d"))
  }

  test ("Instrument.makeInstrumentsList should accept split each strip whitespace around names") {
    Instrument.makeInstrumentsList("a,b", "  c  , d  ") should equal(makeInstruments("a", "b", "c", "d"))
  }
}
