package org.chicagoscala.awse.util
import se.scalablesolutions.akka.util.Logging

case class AkkaWebSampleExerciseException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}

sealed trait LogAndThrow {
  protected def _log(cause: Throwable, message: String)
  protected def _log(message: String)

  def apply(cause: Throwable, message: String) = {
    _log(cause, message)
    throw cause
  }

  def apply(cause: Throwable) = {
    _log(cause, "")
    throw cause
  }

  def apply(message: String) = {
    _log(message)
    throw new AkkaWebSampleExerciseException(message)
  }
}

object error extends LogAndThrow with Logging {
  protected def _log(cause: Throwable, message: String) = log.error(cause, message)
  protected def _log(message: String) = log.error(message)
}

object fatal extends LogAndThrow with Logging {
  protected def _log(cause: Throwable, message: String) = log.fatal(cause, message)
  protected def _log(message: String) = log.fatal(message)
}
