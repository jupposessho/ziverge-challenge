package com.ziverge.service

import java.io.IOException

import com.ziverge.config.Configuration.StreamConfig
import com.ziverge.model.InputRecord
import io.circe.parser.decode
import os.Shellable
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.stream.ZStream
import zio.stream.ZTransducer._

object WordsStream {

  trait Service {
    def stream(command: Shellable*): ZStream[Clock with Console, IOException, Unit]
  }

  def apply(config: StreamConfig, countService: CountService.Service) = new Service {

    def stream(command: Shellable*): ZStream[Clock with Console, IOException, Unit] = {
      ZStream
        .effectAsync[Console, Nothing, String] { cb =>
          os.proc(command)
            .spawn(stdout = os.ProcessOutput.Readlines(l => cb(ZIO.succeed(Chunk(l)))))
          ()
        }
        .map(line => decode[InputRecord](line).toOption)
        .tap(e => e.fold(UIO.unit)(countService.appendCurrent))
        .aggregateAsyncWithin(collectAllN(config.batchSize), Schedule.fixed(config.interval))
        .mapM(countService.saveBatch)
    }
  }
}
