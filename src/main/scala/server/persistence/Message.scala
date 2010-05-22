package org.chicagoscala.awse.server.persistence
import org.joda.time._

sealed trait Message

case class Get(startTime: DateTime, endTime: DateTime) extends Message 
  
case class Put(time: DateTime, json: String) extends Message

case object Stop extends Message
