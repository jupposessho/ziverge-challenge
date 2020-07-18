package com.ziverge.service

import com.ziverge.model.CountState.StateType
import com.ziverge.model.{BatchCount, CountResponse, EventCount, InputRecord, WordCount}
import com.ziverge.repository.CountRepository
import zio._
import zio.clock.Clock

import java.util.concurrent.TimeUnit
import scala.collection.immutable.HashMap
import com.ziverge.model.CountState

object CountService {

  trait Service {
    def saveBatch(records: List[Option[InputRecord]]): UIO[Unit]
    def appendCurrent(record: InputRecord): UIO[Unit]
    def counts(): UIO[CountResponse]
  }

  def apply(repository: CountRepository.Service, clock: Clock.Service) = {
    new Service {
      override def saveBatch(records: List[Option[InputRecord]]): UIO[Unit] = {
        val eventCounts = calculateBatchCount(records)
        for {
          _ <- repository.resetCurrent()
          now <- clock.currentTime(TimeUnit.MILLISECONDS)
          _ <- repository.updateHistory(BatchCount(eventCounts, now))
        } yield ()
      }

      override def counts(): UIO[CountResponse] =
        for {
          now <- clock.currentTime(TimeUnit.MILLISECONDS)
          current <- repository.current()
          history <- repository.history()
        } yield CountResponse(if (current == CountState.empty) history else BatchCount(eventCounts(current), now) :: history)

      def appendCurrent(record: InputRecord): UIO[Unit] = repository.appendCurrent(record)
    }
  }

  private def eventCounts(state: StateType): List[EventCount] = {
    state
      .map { eventCount => (eventCount._1._1, eventCount._1._2, eventCount._2) }
      .groupBy(_._1)
      .map { eventType =>
        val list = eventType._2.map(e => WordCount(e._2, e._3)).toList
        EventCount(eventType._1, list)
      }
      .toList
  }

  private def calculateBatchCount(records: List[Option[InputRecord]]): List[EventCount] = {
    val state = records.flatten.foldLeft(HashMap.empty[(String, String), Int]) { (map, record) =>
      map.updatedWith(record.event_type -> record.data) { keys => keys.map(_ + 1).orElse(Some(1)) }
    }

    eventCounts(state)
  }
}
