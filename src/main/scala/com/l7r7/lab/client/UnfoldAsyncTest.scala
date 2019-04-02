package com.l7r7.lab.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }

object UnfoldAsyncTest {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  def read(initialRequest: HttpRequest): Source[HttpResponse, _] =
    Source.unfoldAsync[HttpRequest, HttpResponse](initialRequest)(crawl)

  def createRequest() = HttpRequest(uri = "http://localhost:8080/content")

  def main(args: Array[String]): Unit = {
    val initialRequest: HttpRequest = createRequest()
    read(initialRequest)
      .take(1000)
      .mapAsync(1)(response => Unmarshal(response).to[Multipart.General])
      .flatMapConcat(_.parts)
      .map(_.entity.contentType)
      .zipWithIndex
      .runForeach(println)
  }

  def crawl(request: HttpRequest)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): Future[Option[(HttpRequest, HttpResponse)]] = {
    Http().singleRequest(request)
      .map(response => Some(request, response))
  }
}
