package com.sebastianvoss

import java.net.InetAddress

import akka.actor.{Props, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{RemoteAddress, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
 * Created by sebastian on 14.04.16.
 */
object SampleServer extends App {
  implicit val system = ActorSystem("akka-rate-limiter")
  val helloActor = system.actorOf(Props[HelloActor])
  val rateLimiter = system.actorOf(Props(new RateLimitActor(10000, .02, 300, 10, 100, helloActor)).withDispatcher("priority-dispatcher"))

  // takes the list of transformations and materializes them in the form of org.reactivestreams.Processor instances
  implicit val materializer = ActorMaterializer()

  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val route = path("sample") {
    get {
      extractClientIP { clientAddress =>
        parameters('name ? "World") { name =>
          val ip = clientAddress.toOption match {
            case Some(a: InetAddress) => a.getHostAddress
            case _ => "unknown"
          }
          val message = (ip, name)
          val future = rateLimiter ? message
          onComplete(future) {
            case Success(response) => complete(StatusCodes.Accepted -> s"$response\n")
            case Failure(ex: RateLimitExceededException) => complete(StatusCodes.TooManyRequests -> "Rate limit exceeded\n")
            case Failure(ex) => complete(StatusCodes.InternalServerError -> "The requested service is currently not available\n")
          }
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ {
    system.terminate()
  })
}
