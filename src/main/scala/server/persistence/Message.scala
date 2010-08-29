package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.domain.finance._

sealed trait Message

case class Get(criteria: Map[String,Any]) extends Message

case class Put(jsonRecord: JSONRecord) extends Message

case object Stop extends Message
