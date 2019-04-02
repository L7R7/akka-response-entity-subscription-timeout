package com.l7r7.lab.server

import java.io.BufferedInputStream

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentType, HttpEntity, MediaType }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object FeedServer {
  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("server")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route =
      get {
        val mediaType = MediaType.customMultipart("package", Map("boundary" -> "gc0p4Jq0M2Yt08jU534c0p", "encoding" -> "UTF-8"))
        val contentType = ContentType(mediaType)
        complete(HttpEntity(contentType, Content.payload))
      }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}

object Content {
  def readFromFile(name: String): Array[Byte] = {
    val bis = new BufferedInputStream(getClass.getResourceAsStream(name))
    try {
      Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
    } finally {
      bis.close()
    }
  }

  val payload: Array[Byte] = readFromFile("/content.txt")
}
