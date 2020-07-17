package com.ziverge

import com.ziverge.config.Configuration
import com.ziverge.model.{BatchCount, CountState}
import com.ziverge.repository.CountRepository
import com.ziverge.routes.CountRoutes
import com.ziverge.service.{CountService, WordsStream}
import com.ziverge.service.Bootstrap._
import scala.collection.immutable.HashMap
import zio._
import zio.clock.Clock
import zio.console._
import zio.stream.ZStream

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {

    def program(file: String) =
      for {
        config <- Configuration.load()
        source <- open(file)
        currentRef <- Ref.make(HashMap.empty[(String, String), Int]).toManaged_
        historyRef <- Ref.make(List.empty[BatchCount]).toManaged_
        countState = CountState(currentRef, historyRef)
        service = CountService(CountRepository(countState), Clock.Service.live)
        routes = CountRoutes(service).routes()
        _ <- ZStream
          .mergeAllUnbounded()(ZStream.fromEffect(
                                 WordsStream(config.streamConfig, service)
                                   .stream(source)
                               ),
                               ZStream.fromEffect(server(config.server, routes)))
          .runDrain
          .toManaged_
      } yield ()

    args match {
      case file :: Nil =>
        program(file).use_(ZIO.unit).exitCode
      case _ =>
        putStrLn("Missing filename argument") *> ZIO.succeed(ExitCode.failure)
    }
  }
}
