package org.chicagoscala.awse.math

/**
 * Calculate prime numbers, which we use to provide the actors something to do.
 * Adapted from section 1.2.6 of "The Structure and Longerpretation of Computer Programs".
 */
object Primes {
  
  protected def findDivisor(n: Long, testDivisor: Long): Long = {
    if (testDivisor * testDivisor > n) n
    else if (divides(testDivisor, n)) testDivisor
    else findDivisor(n, testDivisor + 1)
  }

  def smallestDivisor(n: Long) = findDivisor(n, 2)

  def divides(testDivisor: Long, n: Long) = n % testDivisor == 0

  def prime(n: Long) = smallestDivisor(n) == n

  def apply(start: Long, end: Long) = 
    (for { 
      i <- start to end
      if prime(i)
    } yield i) toList 
}
