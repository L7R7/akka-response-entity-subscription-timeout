package com.l7r7.lab.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Multipart }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

object WorkingUnfoldAsync {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  def read(initialRequest: HttpRequest): Source[Multipart.General, _] =
    Source.unfoldAsync[HttpRequest, Multipart.General](initialRequest)(crawl)

  def createRequest() = HttpRequest(uri = "http://localhost:8080/content")

  def main(args: Array[String]): Unit = {
    val initialRequest: HttpRequest = createRequest()
    read(initialRequest)
      //      .async // this will break it
      .flatMapConcat(_.parts)
      .map(_.entity.contentType)
      .zipWithIndex
      .runForeach(println)
      .onComplete {
        case Success(_) => println("done")
        case Failure(e) => println(s"error: $e")
      }
  }

  def crawl(request: HttpRequest)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): Future[Option[(HttpRequest, Multipart.General)]] =
    Http().singleRequest(request)
      .flatMap(response => Unmarshal(response).to[Multipart.General])
      .map(multipart => Some(request, multipart))
}
