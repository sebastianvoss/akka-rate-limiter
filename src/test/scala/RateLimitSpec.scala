import akka.actor.Status.Failure
import akka.actor.{Props, ActorSystem, Actor}
import akka.testkit.{DefaultTimeout, TestActors, TestKit, ImplicitSender}
import com.sebastianvoss.{RateLimitExceededException, HelloActor, RateLimitActor}
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import akka.pattern.ask
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await

/**
 * Created by sebastian on 19.04.16.
 */
class RateLimitSpec extends TestKit(ActorSystem("RateLimitSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with DefaultTimeout {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A RateLimit actor" must {

    "forward messages from same client to the target actor when not exceeding the rate limit" in {
      val targetActor = system.actorOf(TestActors.echoActorProps)
      val rateLimitActor = system.actorOf(Props(new RateLimitActor(100, .01, 100, 10, 3, targetActor)).withDispatcher("priority-dispatcher"))
      val message = ("127.0.0.1", "Test")
      val messages = List.fill(3)(message)
      messages.foreach(rateLimitActor ! _)
      expectMsgAllOf(messages.map(_._2): _*)
    }

    "forward messages from different clients to the target actor" in {
      val targetActor = system.actorOf(TestActors.echoActorProps)
      val rateLimitActor = system.actorOf(Props(new RateLimitActor(10000, .02, 100, 10, 3, targetActor)).withDispatcher("priority-dispatcher"))
      val messages = for (i <- 1 to 10000) yield (s"user$i", s"Test$i")
      messages.foreach(rateLimitActor ! _)
      expectMsgAllOf(messages.map(_._2): _*)
    }

    "fail with RateLimitExceededException when exceeding the rate limit" in {
      val targetActor = system.actorOf(TestActors.echoActorProps)
      val rateLimitActor = system.actorOf(Props(new RateLimitActor(100, .01, 100, 10, 2, targetActor)).withDispatcher("priority-dispatcher"))
      val message = ("127.0.0.1", "Test")
      rateLimitActor ! message
      rateLimitActor ! message
      expectMsgAllOf(message._2, message._2)
      val future = rateLimitActor ? message
      future should be('completed)
      intercept[RateLimitExceededException] {
        Await.result(future, timeout.duration)
      }
      expectNoMsg()
    }

    "forward all message to the target actor when not exceeding the rate limit across sliding windows" in {
      val targetActor = system.actorOf(TestActors.echoActorProps)
      val rateLimitActor = system.actorOf(Props(new RateLimitActor(100, .01, 2, 1, 4, targetActor)).withDispatcher("priority-dispatcher"))
      val message = ("127.0.0.1", "Test")
      val messages = List.fill(6)(message)
      messages.foreach(m => {
        Thread.sleep(500)
        rateLimitActor ! m
      })
      expectMsgAllOf(messages.map(_._2): _*)
    }

  }
}