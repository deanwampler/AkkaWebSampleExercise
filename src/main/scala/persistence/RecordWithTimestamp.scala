package org.chicagoscala.awse.persistence

/**
 * The type parameters allowed for DataStore.
 */
trait RecordWithTimestamp[TS] {
  def timestamp: TS
}

