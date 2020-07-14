package com.ziverge.service

import com.ziverge.model.InputRecord
import io.circe.parser.decode
import java.io.File
import zio.duration._
import zio._
import zio.blocking._
import zio.stream.ZStream
import zio.stream.ZTransducer._
import java.io.{IOException, InputStream}

object WordsStream {

  def stream(file: String, countService: CountService.Service, blocking: Blocking.Service) = {
    import blocking.effectBlocking
    
    ZStream
      .fromInputStreamManaged(open(file))
      .transduce(utf8Decode >>> splitLines)
      .map(line => decode[InputRecord](line).toOption)
      .aggregateAsyncWithin(collectAllN(10), Schedule.fixed(10.seconds))
      .foreach(p => countService.save(p))
  }

  private def open(file: String) = {
    val acquire = effectBlocking(Tail.follow(new File(file))).refineToOrDie[IOException]
    val release = (input: InputStream) => effectBlocking(input.close()).orDie

    Managed.make(acquire)(release)
  }
}
