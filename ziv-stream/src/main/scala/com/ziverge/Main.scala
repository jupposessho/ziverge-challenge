package com.ziverge

import com.ziverge.config.Configuration
import com.ziverge.model.{BatchCount, CountState}
import com.ziverge.repository.CountRepository
import com.ziverge.routes.CountRoutes
import com.ziverge.service.{CountService, WordsStream}
import com.ziverge.service.Bootstrap._
import java.nio.file.Paths
import os.Path
import zio._
import zio.clock.Clock
import zio.console._
import zio.stream.ZStream

object Main extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {

    def program(command: String) =
      for {
        config <- Configuration.load()
        currentRef <- Ref.make(CountState.empty).toManaged_
        historyRef <- Ref.make(List.empty[BatchCount]).toManaged_
        countState = CountState(currentRef, historyRef)
        service = CountService(CountRepository(countState), Clock.Service.live)
        routes = CountRoutes(service).routes()
        _ <- ZStream
          .mergeAllUnbounded()(WordsStream(config.streamConfig, service).stream(command),
                               ZStream.fromEffect(server(config.server, routes)))
          .runDrain
          .toManaged_
      } yield ()

    args match {
      case command :: Nil =>
        ZIO.effect(Path(Paths.get(command))).flatMap { path =>
          if (os.exists(path)) program(command).use_(ZIO.unit)
          else putStrLn(s"Couldn't find command: $command")
        }
        .orElse(putStrLn(s"Invalid path: $command")) *> ZIO.succeed(ExitCode.failure)
      case _ =>
        putStrLn("Missing command argument") *> ZIO.succeed(ExitCode.failure)
    }
  }
}
