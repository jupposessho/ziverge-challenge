package com.ziverge.service

import com.ziverge.config.Configuration.StreamConfig
import com.ziverge.model.InputRecord
import io.circe.parser.decode
import java.io.{File, IOException, InputStream}
import zio._
import zio.duration._
import zio.blocking._
import zio.clock.Clock
import zio.stream.ZStream
import zio.stream.ZTransducer._

object WordsStream {

  trait Service {
    def stream(file: String): ZIO[Blocking with Clock, Throwable, Unit]
  }

  def apply(config: StreamConfig, countService: CountService.Service, blocking: Blocking.Service) = new Service {
    import blocking.effectBlocking

    def stream(file: String) = {

      ZStream
        .fromInputStreamManaged(open(file))
        .transduce(utf8Decode >>> splitLines)
        .map(line => decode[InputRecord](line).toOption)
        .aggregateAsyncWithin(collectAllN(config.batchSize), Schedule.fixed(config.interval))
        .foreach(p => countService.saveBatch(p))
    }

    private def open(file: String) = {
      val acquire = effectBlocking(Tail.follow(new File(file))).refineToOrDie[IOException]
      val release = (input: InputStream) => effectBlocking(input.close()).orDie

      Managed.make(acquire)(release)
    }
  }
}
