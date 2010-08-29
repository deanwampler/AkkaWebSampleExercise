package org.chicagoscala.awse.util
import se.scalablesolutions.akka.util.Logging
import net.lag.logging.Level

/**
 * Invoke a by-name parameter and log its result. 
 * The format string should accept up to one parameter, a string created by
 * calling <tt>toString</tt> on the value returned by the block.
 * Note: Will not log a message if an exception is thrown.
 * TODO: Verify that it works with actor reply(...) results.
 */
case class WithLogMessage[T](level: Level, msgFormat: String) extends Logging {
  def apply (block: => T): T = {
    val t = block
    log.log(level, String.format(msgFormat, t.toString))
    t
  }
}
