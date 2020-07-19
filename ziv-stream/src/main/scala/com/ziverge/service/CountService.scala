package com.ziverge.service

import com.ziverge.model.CountState.StateType
import com.ziverge.model.{BatchCount, CountResponse, EventCount, InputRecord, WordCount}
import com.ziverge.repository.CountRepository
import zio._
import zio.clock.Clock
import java.util.concurrent.TimeUnit

import scala.collection.immutable.HashMap
import com.ziverge.model.CountState
import zio.stream.ZStream

object CountService {

  trait Service {
    def saveBatch(records: List[Option[InputRecord]]): UIO[Unit]
    def appendCurrent(record: InputRecord): UIO[Unit]
    def counts(): UIO[CountResponse]
  }

  def apply(repository: CountRepository.Service, clock: Clock.Service) = {
    new Service {
      override def saveBatch(records: List[Option[InputRecord]]): UIO[Unit] = {
        for {
          eventCounts <- calculateBatchCount(records)
          _ <- repository.resetCurrent()
          now <- clock.currentTime(TimeUnit.MILLISECONDS)
          _ <- repository.updateHistory(BatchCount(eventCounts.toList, now))
        } yield ()
      }

      override def counts(): UIO[CountResponse] =
        for {
          now <- clock.currentTime(TimeUnit.MILLISECONDS)
          current <- repository.current()
          history <- repository.history()
          batches <- if (current == CountState.empty) UIO.succeed(history) else eventCounts(current).map(e => BatchCount(e.toList, now) :: history)
        } yield CountResponse(batches)

      def appendCurrent(record: InputRecord): UIO[Unit] = repository.appendCurrent(record)
    }
  }

  private def eventCounts(state: StateType) = {
    ZStream
      .fromIterable(state)
      .groupByKey(_._1._1)
      .apply {
        case (eventType, subStream) =>
          val wordCounts = subStream.collect {
            case ((_, word), count) =>
              WordCount(word, count)
          }

          ZStream.fromEffect(wordCounts.runCollect.map(e => EventCount(eventType, e.toList)))
      }
      .runCollect
  }

  private def calculateBatchCount(records: List[Option[InputRecord]]): UIO[Chunk[EventCount]] =
    for {
      events <- ZStream
        .fromIterable(records)
        .collect {
          case Some(r) => r
        }
        .fold(HashMap.empty[(String, String), Int]) { (state, inputRecord) =>
          state.updatedWith(inputRecord.eventType -> inputRecord.data) { _.map(_ + 1).orElse(Some(1)) }
        }
      eventCounts <- eventCounts(events)
    } yield eventCounts
}
