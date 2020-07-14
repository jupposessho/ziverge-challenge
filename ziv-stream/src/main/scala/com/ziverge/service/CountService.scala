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
        val wordCounts = eventCount._2.map(e => WordCount(e._1, e._2)).toList
        eventCount._1 -> wordCounts
      }
      .map(e => EventCount(e._1, e._2))
      .toList

    CountResponse(counts)
  }

  private def calculateCount(records: List[Option[InputRecord]]): StateType = {
    records.flatten.foldLeft(HashMap.empty[String, HashMap[String, Int]]) { (map, record) =>
      map.updatedWith(record.event_type) { eventType =>
        eventType
          .map { countsForType =>
            countsForType.updatedWith(record.data) { wordCount =>
              wordCount.map(_ + 1).orElse(Some(1))
            }
          }
          .orElse(Some(HashMap(record.data -> 1)))
      }
    }
  }
}
