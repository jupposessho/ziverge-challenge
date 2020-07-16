package com.ziverge.service

import com.ziverge.model.CountState.StateType
import com.ziverge.model.{CountResponse, EventCount, InputRecord, WordCount}
import com.ziverge.repository.CountRepository
import zio._

import scala.collection.immutable.HashMap

object CountService {

  trait Service {
    def save(records: List[Option[InputRecord]]): Task[Unit]
    def count(): Task[CountResponse]
  }

  def apply(repository: CountRepository.Service) = {
    new Service {
      override def save(records: List[Option[InputRecord]]): Task[Unit] =
        repository.set(calculateCount(records))

      override def count(): Task[CountResponse] =
        repository.get().map(toResponse)
    }
  }

  private def toResponse(state: StateType): CountResponse = {

    val counts = state
      .map { eventCount =>
        (eventCount._1._1, eventCount._1._2, eventCount._2)
      }
      .groupBy(_._1)
      .map { eventType =>
        val list = eventType._2.map(e => WordCount(e._2, e._3)).toList
        EventCount(eventType._1, list)
      }
      .toList

    CountResponse(counts)
  }

  private def calculateCount(records: List[Option[InputRecord]]): StateType = {
    records.flatten.foldLeft(HashMap.empty[(String, String), Int]) { (map, record) =>
      map.updatedWith(record.event_type -> record.data) { keys => keys.map(_ + 1).orElse(Some(1)) }
    }
  }
}
