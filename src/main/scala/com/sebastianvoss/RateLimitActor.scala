package com.sebastianvoss

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorLogging, ActorSystem, Actor}
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config
import scala.concurrent.duration._

/**
 * Created by sebastian on 16.04.16.
 */
case class SlideWindow()

class RateLimitExceededException extends Exception

class PrioritizedMailbox(settings: ActorSystem.Settings, cfg: Config)
  extends UnboundedPriorityMailbox(
    PriorityGenerator {
      case SlideWindow => 0
      case _ => 10
    })

/** A rate limiter based on counting bloom filters
  *
  * @constructor create a new rate limiter actor
  * @param numRequests expected number of requests within batchLength
  * @param falsePositiveRate desired false positive rate (between 0 and 1)
  * @param slidingWindowLength length of the sliding window in seconds
  * @param batchLength length of a batch in seconds
  * @param rateLimit maximum number of requests per slidingWindowLength
  * @param target actor which will receive the messages when rate limit is not exceeded
  */
class RateLimitActor(numRequests: Int, falsePositiveRate: Double, slidingWindowLength: Int, batchLength: Int, rateLimit: Int, target: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  private val tick = context.system.scheduler.schedule(batchLength seconds, batchLength seconds, self, SlideWindow())

  val m = (-(numRequests * math.log(falsePositiveRate)) / math.pow(math.log(2), 2)).toInt
  val k = (m / numRequests * math.log(2)).toInt
  val numFilters = slidingWindowLength / batchLength
  val filters = CircularArray[CountingBloomFilter](numFilters)

  override def preStart() = {
    log.debug(
      s"Starting RateLimitActor (numOfBloomFilters: $numFilters, m=$m, k=$k, rateLimit=$rateLimit)")
    filters.add(CountingBloomFilter(m, k))
  }

  override def postStop() = tick.cancel()

  override def receive = {
    case (ip: String, request) => {
      val c = count(ip)
      if (c < rateLimit) {
        log.debug(s"processing request ${c + 1} from $ip")
        filters.last.add(ip)
        target.forward(request)
      } else {
        log.debug(s"dropping request from $ip")
        sender ! Failure(new RateLimitExceededException())
      }
    }
    case SlideWindow() => {
      log.debug("sliding window")
      filters.add(CountingBloomFilter(m, k))
    }
    case _ => {
      log.warning("received invalid request")
      sender ! Failure(new Exception("Invalid request"))
    }
  }

  private def count(value: String): Int = {
    val hashes = CountingBloomFilter.hashes(m, k)(value)
    filters.buffer.foldLeft(0)((m: Int, f: CountingBloomFilter) => f match {
      case f: CountingBloomFilter => m + f.count(hashes)
      case _ => m
    })
  }
}
