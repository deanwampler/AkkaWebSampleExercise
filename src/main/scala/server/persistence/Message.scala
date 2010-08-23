package org.chicagoscala.awse.server.persistence
import org.joda.time._

sealed trait Message

case class Get(min: Long = 0, max: Long = java.lang.Long.MAX_VALUE) extends Message 
  
case class Put(json: String) extends Message

case object Stop extends Message
