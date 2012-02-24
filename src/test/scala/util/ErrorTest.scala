package org.chicagoscala.awse.util
import org.scalatest.{FlatSpec, FunSuite, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers

class ErrorTest extends FunSuite with ShouldMatchers with BeforeAndAfterAll {
  override def beforeAll(configMap: Map[String, Any]) {
    Logging.disable
  }
  override def afterAll(configMap: Map[String, Any]) {
    Logging.enable
  }
  
  test ("error(throwable, message) logs the throwable and message as an error.") {
    val th = new RuntimeException("error thrown")
    val msg = "error message"
    val count = Logging.count
    intercept[RuntimeException] { error (th, msg) }
    Logging.count should equal (count + 1)
  }

  test ("error(throwable, message) throws the throwable.") {
    val th = new RuntimeException("error thrown")
    val msg = "error message"
    intercept[RuntimeException] { error (th, msg) }    
  }

  test ("error(throwable) logs the throwable as an error.") {
    val th = new RuntimeException("error thrown")
    val count = Logging.count
    intercept[RuntimeException] { error (th) }
    Logging.count should equal (count + 1)
  }

  test ("error(throwable) throws the throwable.") {
    val th = new RuntimeException("error thrown")
    intercept[RuntimeException] { error (th) }    
  }

  test ("error(message) logs the message as an error.") {
    val msg = "error message"
    val count = Logging.count
    intercept[GeneralAppException] { error (msg) }
    Logging.count should equal (count + 1)
  }

  test ("error(message) throws a GeneralAppException.") {
    val msg = "error message"
    intercept[GeneralAppException] { error (msg) }    
  }

  test ("fatal(throwable, message) logs the throwable and message as a fatal.") {
    val th = new RuntimeException("fatal thrown")
    val msg = "fatal message"
    val count = Logging.count
    intercept[RuntimeException] { fatal (th, msg) }
    Logging.count should equal (count + 1)
  }

  test ("fatal(throwable, message) throws the throwable.") {
    val th = new RuntimeException("fatal thrown")
    val msg = "fatal message"
    intercept[RuntimeException] { fatal (th, msg) }    
  }

  test ("fatal(throwable) logs the throwable as a fatal.") {
    val th = new RuntimeException("fatal thrown")
    val count = Logging.count
    intercept[RuntimeException] { fatal (th) }
    Logging.count should equal (count + 1)
  }

  test ("fatal(throwable) throws the throwable.") {
    val th = new RuntimeException("fatal thrown")
    intercept[RuntimeException] { fatal (th) }    
  }

  test ("fatal(message) logs the message as a fatal.") {
    val msg = "fatal message"
    val count = Logging.count
    intercept[GeneralAppException] { fatal (msg) }
    Logging.count should equal (count + 1)
  }

  test ("fatal(message) throws a GeneralAppException.") {
    val msg = "fatal message"
    intercept[GeneralAppException] { fatal (msg) }    
  }
}
