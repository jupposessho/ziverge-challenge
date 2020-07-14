package com.ziverge

import com.ziverge.model.CountState
import com.ziverge.service.{CountService, WordsStream}
import com.ziverge.repository.CountRepository
import com.ziverge.routes.CountRoutes
import org.http4s.server.blaze._
import org.http4s.HttpApp

import scala.collection.immutable.HashMap
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.stream.ZStream
import zio.blocking.Blocking

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {

     val program = for {
       ref <- Ref.make(HashMap.empty[String, HashMap[String, Int]])
       countState = CountState(ref)
       service = CountService(CountRepository(countState))
       routes = CountRoutes(service).routes()
       _ <- WordsStream.stream("/Users/feherv/Downloads/ziv.txt", service, Blocking.Service.live).fork
       _ <- server(routes)
       _ <- ZIO.never
     } yield ()

     program
       .exitCode
  }

  def server(routes: HttpApp[Task]) = ZIO.runtime[ZEnv]
      .flatMap {
        implicit rts =>
          BlazeServerBuilder[Task]
            .bindHttp(8080, "localhost")
            .withHttpApp(routes)
            .serve
            .compile
            .drain
      }
}
