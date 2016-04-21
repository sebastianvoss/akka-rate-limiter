package com.sebastianvoss

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
 * Created by sebastian on 17.04.16.
 */
class HelloActor extends Actor {
  override def receive = {
    case s: String => sender ! s"Hello $s"
    case _ => sender ! "Hello World"
  }
}
