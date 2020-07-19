package com.ziverge.service

import com.ziverge.config.Configuration.StreamConfig
import com.ziverge.model.InputRecord
import io.circe.parser.decode
import java.io.{IOException, InputStream}

import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.stream.ZStream
import zio.stream.ZTransducer._

object WordsStream {

  trait Service {
    def stream(input: InputStream): ZStream[Blocking with Clock, IOException, List[Option[InputRecord]]]
  }

  def apply(config: StreamConfig, countService: CountService.Service) = new Service {

    def stream(input: InputStream): ZStream[Blocking with Clock, IOException, List[Option[InputRecord]]] = {
      ZStream
        .fromInputStream(input)
        .transduce(utf8Decode >>> splitLines)
        .map(line => decode[InputRecord](line).toOption)
        .tap(e => e.fold(UIO.unit)(countService.appendCurrent))
        .aggregateAsyncWithin(collectAllN(config.batchSize), Schedule.fixed(config.interval))
        .tap(p => countService.saveBatch(p))
    }
  }
}
