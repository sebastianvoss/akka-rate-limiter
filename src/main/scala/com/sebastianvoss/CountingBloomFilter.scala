package com.sebastianvoss

import scala.util.hashing.MurmurHash3

/**
 * Created by sebastian on 16.04.16.
 */
class CountingBloomFilter(m: Int, k: Int) {
  val filter = new Array[Int](m)

  def add(value: String) = {
    val h = CountingBloomFilter.hashes(m, k)(value)
    h.foreach(filter(_) += 1)
  }

  def count(value: String): Int = {
    val h = CountingBloomFilter.hashes(m, k)(value)
    count(h)
  }

  def count(hashes: Array[Int]): Int = hashes.map(i => filter(i)).min
}

object CountingBloomFilter {
  def apply(m: Int, k: Int) = {
    new CountingBloomFilter(m, k)
  }

  /** Creates k MurmurHash3 hashes using double hashing
    * (https://en.wikipedia.org/wiki/Double_hashing)
    *
    * @param m size of the bloom filter
    * @param k number of hashes to be generated
    * @param value value for which the hashes need be to generated
    * @return array of k hashes
    */
  def hashes(m: Int, k: Int)(value: String): Array[Int] = {
    val hash1 = MurmurHash3.stringHash(value, 0)
    val hash2 = MurmurHash3.stringHash(value, hash1)
    (for (i <- 0 until k) yield Math.abs((hash1 + i * hash2) % m)).toArray
  }
}
