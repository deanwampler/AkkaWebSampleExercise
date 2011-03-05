package org.chicagoscala.awse.util
import akka.util.{Logging => ALogging}

/**
 * Wrapper around Akka's logging to better support suppression during tests and
 * to add a single log(level, ...) method, for programmatic determinition of the
 * logging level.
 * Notes:
 * 1) Akka's API doesn't have a log.fatal call. Nor does it have log methods that
 *    take just a Throwable without a message.
 */
trait Logging extends ALogging {
  
  import Logging._
  import Logging.Levels._
  
  def logAt(level: Level, cause: Throwable, message: String): Unit = {
    incrementCount
    if (enabled)
      level match {
        case Debug   => log.debug  (cause, message)
        case Trace   => log.trace  (cause, message)
        case Info    => log.info   (cause, message)
        case Warning => log.warning(cause, message)
        case Error | Fatal => log.error(cause, message)
      } 
  }
  
  def logAt(level: Level, cause: Throwable): Unit = {
    incrementCount
    if (enabled)
      level match {
        case Debug   => log.debug  (cause, "")
        case Trace   => log.trace  (cause, "")
        case Info    => log.info   (cause, "")
        case Warning => log.warning(cause, "")
        case Error | Fatal => log.error(cause, "")
      }
  }
  
  def logAt(level: Level, message: String): Unit = {
    incrementCount
    if (enabled)
      level match {
        case Debug   => log.debug  (message)
        case Trace   => log.trace  (message)
        case Info    => log.info   (message)
        case Warning => log.warning(message)
        case Error | Fatal => log.error(message)
      }
  }
}

object Logging {
  object Levels extends Enumeration {
    type Level = Value
    val Debug, Trace, Info, Warning, Error, Fatal = Value
  }

  def enable  = isDisabled = false
  def disable = isDisabled = true
  def enabled = ! disabled
  def disabled = isDisabled

  var isDisabled = false

  /**
   * Total count of calls to Logging.logAt methods. Used primarily for testing.
   */
  def count = _count
  def incrementCount = _count += 1
  
  protected var _count = 0
}