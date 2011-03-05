package org.chicagoscala.awse.util

/**
 * This exception doesn't really add anything to RuntimeException, 
 * except that it is only thrown in this application's code vs. any
 * third-party code.
 */
case class GeneralAppException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}

sealed trait LogAndThrow extends Logging {
  val level: Logging.Levels.Level
  
  def apply(cause: Throwable, message: String) = {
    logAt(level, cause, message)
    throw cause
  }

  def apply(cause: Throwable) = {
    logAt(level, cause)
    throw cause
  }

  def apply(message: String) = {
    logAt(level, message)
    throw new GeneralAppException(message)
  }
}

object error extends LogAndThrow with Logging {
  val level = Logging.Levels.Error
}

object fatal extends LogAndThrow with Logging {
  val level = Logging.Levels.Fatal
}
