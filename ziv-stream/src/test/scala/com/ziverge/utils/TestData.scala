package com.ziverge.utils

import java.time.{DateTimeException, OffsetDateTime}
import java.util.concurrent.TimeUnit

import zio.clock.Clock
import zio.duration.Duration
import zio.{IO, UIO}

import scala.collection.immutable.HashMap

object TestData {

  val emptyState = HashMap.empty[(String, String), Int]

  val fakeNow = 1594934147262L

  def fakeClock(now: Long = fakeNow) = new Clock.Service {
    override def currentTime(unit: TimeUnit): UIO[Long] = UIO.succeed(now)

    override def currentDateTime: IO[DateTimeException, OffsetDateTime] = ???

    override def nanoTime: UIO[Long] = ???

    override def sleep(duration: Duration): UIO[Unit] = ???
  }
}
