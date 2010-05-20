package org.chicagoscala.awse.server
import se.scalablesolutions.akka.actor._

trait NamedActor extends Actor {
  val name: String
}

