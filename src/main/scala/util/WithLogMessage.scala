package org.chicagoscala.awse.util
import akka.util.Logging
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
    val message = String.format(msgFormat, t.toString)
    // TODO: Akka .10 removed the log(level, ...) method. Ask them to add it back!
    // log.log(level, message)
    level match {
      case Level.TRACE   => log trace   message
      case Level.DEBUG   => log debug   message
      case Level.WARNING => log warning message
      case _             => log error   message
    }
    t
  }
}
