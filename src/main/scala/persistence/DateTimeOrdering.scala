package org.chicagoscala.awse.persistence

object DateTimeOrdering extends scala.math.Ordering[DateTime] {
  def compare(d1: DateTime, d2: DateTime) = d1.getMillis compare d2.getMillis
}
