package com.ziverge

import com.ziverge.config.Configuration
import com.ziverge.config.Configuration.ServerConfig
import com.ziverge.model.CountState
import com.ziverge.service.{CountService, WordsStream}
import com.ziverge.repository.CountRepository
import com.ziverge.routes.CountRoutes
import org.http4s.server.blaze._
import org.http4s.HttpApp

import scala.collection.immutable.HashMap
import zio._
import zio.blocking.Blocking
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {

    def program(file: String) =
      for {
        ref <- Ref.make(HashMap.empty[(String, String), Int])
        countState = CountState(ref)
        service = CountService(CountRepository(countState))
        routes = CountRoutes(service).routes()
        config <- Configuration.load().useNow
        _<- WordsStream(config.streamConfig, service, Blocking.Service.live).stream(file).fork
        _ <- server(config.server, routes)
      } yield ()

    args match {
      case file :: Nil =>
        program(file).exitCode
      case _ =>
        putStrLn("Missing filename argument") *> ZIO.succeed(ExitCode.failure)
    }
  }

  def server(config: ServerConfig, routes: HttpApp[Task]) =
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit rts =>
        BlazeServerBuilder[Task]
          .bindHttp(config.port, config.host)
          .withHttpApp(routes)
          .serve
          .compile
          .drain
      }
}
