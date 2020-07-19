package com.ziverge.repository

import com.ziverge.model.{BatchCount, CountState, InputRecord}
import com.ziverge.model.CountState.StateType
import zio.UIO

object CountRepository {

  trait Service {
    def appendCurrent(record: InputRecord): UIO[Unit]
    def updateHistory(batch: BatchCount): UIO[Unit]
    def current(): UIO[StateType]
    def history(): UIO[List[BatchCount]]
    def resetCurrent(): UIO[Unit]
  }

  def apply(countState: CountState) =
    new Service {
      override def appendCurrent(record: InputRecord): UIO[Unit] = {
        countState.current.update(_.updatedWith(record.eventType -> record.data)(_.map(_ + 1).orElse(Some(1))))
      }

      override def current(): UIO[StateType] = countState.current.get
      override def updateHistory(batch: BatchCount): UIO[Unit] = countState.history.update(batch :: _)
      override def history(): UIO[List[BatchCount]] = countState.history.get
      override def resetCurrent(): UIO[Unit] = countState.current.set(CountState.empty)
    }
}
