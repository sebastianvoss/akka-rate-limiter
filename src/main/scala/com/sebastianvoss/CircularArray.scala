package com.sebastianvoss

/**
 * Created by sebastian on 16.04.16.
 */
class CircularArray[T: Manifest](val size: Int) {
  val buffer = new Array[T](size)
  private var cur = 0
  private var next = 0

  def add(value: T) {
    buffer(next) = value
    cur = next
    next = (next + 1) % size
  }

  def last: T = {
    buffer(cur)
  }
}

object CircularArray {
  def apply[T: Manifest](size: Int) = new CircularArray[T](size)
}
