package com.l7r7.lab.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Multipart }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Source }

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success, Try }

object HostConnectionPool {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val poolClientFlow: Flow[(HttpRequest, Unit), (Try[HttpResponse], Unit), Http.HostConnectionPool] = Http().cachedHostConnectionPool[Unit]("localhost", 8080)

  def main(args: Array[String]): Unit = {
    val request: HttpRequest = HttpRequest(uri = "http://localhost:8080/")
    Source.repeat(request)
      .map((_, ()))
      .via(poolClientFlow)
      .map(_._1)
      .collect { case Success(r) => r }
      .mapAsync(1)(response => Unmarshal(response).to[Multipart.General])
      .flatMapConcat(_.parts)
      .map(_.entity.contentType)
      .zipWithIndex
      .runForeach(println)
      .onComplete {
        case Success(_) => println("done")
        case Failure(e) => println(s"error: $e")
      }
  }

}
