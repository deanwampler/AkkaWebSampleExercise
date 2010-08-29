package org.chicagoscala.awse.persistence

/**
 * The type parameters allowed for DataStore.
 */
trait RecordWithTimestamp {
  def timestamp: Long
}

