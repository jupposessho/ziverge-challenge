package com.ziverge.service

import com.ziverge.config.Configuration.StreamConfig
import com.ziverge.model.InputRecord
import io.circe.parser.decode
import zio._
import zio.clock.Clock
import zio.blocking.Blocking
import zio.stream.ZStream
import zio.stream.ZTransducer._
import java.io.InputStream

object WordsStream {

  trait Service {
    def stream(input: InputStream): ZIO[Blocking with Clock, Throwable, Unit]
  }

  def apply(config: StreamConfig, countService: CountService.Service) = new Service {

    def stream(input: InputStream) = {

      ZStream
        .fromInputStream(input)
        .transduce(utf8Decode >>> splitLines)
        .map(line => decode[InputRecord](line).toOption)
        .tap(e => e.fold(UIO.unit)(i => countService.appendCurrent(i).orElse(UIO.unit)))
        .aggregateAsyncWithin(collectAllN(config.batchSize), Schedule.fixed(config.interval))
        .foreach(p => countService.saveBatch(p))
    }
  }
}
