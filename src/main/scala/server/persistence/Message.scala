package org.chicagoscala.awse.server.persistence
import org.chicagoscala.awse.persistence._
import org.chicagoscala.awse.domain.finance._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

sealed trait Message

/** 
 * Get data corresponding to criteria encoded in the given Map. The interpretation
 * of the map contents is specified by the DataStorageServer and the underlying DataStores.
 * The structure of the map is intended to be easily serializable to JSON.
 */
case class Get(criteria: Map[String, Any]) extends Message

/**
 * Put a "JSONRecord", a JSON object with a single timestamp, in the persistent storage.
 */
case class Put(jsonRecord: JSONRecord) extends Message

/**
 * Stop what you're doing...
 */
case object Stop extends Message
