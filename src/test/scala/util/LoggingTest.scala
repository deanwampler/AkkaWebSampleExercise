package org.chicagoscala.awse.util
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers

class LoggingTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {
  val logger = new Logging {}
  
  override def beforeAll(configMap: Map[String, Any]) {
    println("NOTE: LoggingTest generates lots of output.")
  }

  test("logAt(level, throwable, message) logs the throwable and message at the specified level") {
    val th = new RuntimeException("exception thrown")
    val msg = "error message"
    val count = Logging.count
    for {
      level <- Logging.Levels.values
    } yield logger.logAt(level, th, msg)
    Logging.count should equal (count + Logging.Levels.values.size)
  }

  test("logAt(level, throwable) logs the throwable at the specified level") {
    val th = new RuntimeException("exception thrown")
    val count = Logging.count
    for {
      level <- Logging.Levels.values
    } yield logger.logAt(level, th)
    Logging.count should equal (count + Logging.Levels.values.size)
  }

  test("logAt(level, message) logs the message at the specified level") {
    val msg = "error message"
    val count = Logging.count
    for {
      level <- Logging.Levels.values
    } yield logger.logAt(level, msg)
    Logging.count should equal (count + Logging.Levels.values.size)
  }
}