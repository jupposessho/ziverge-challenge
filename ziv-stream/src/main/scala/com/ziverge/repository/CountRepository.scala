package com.ziverge.repository

import com.ziverge.model.{BatchCount, CountState, InputRecord}
import com.ziverge.model.CountState.StateType
import zio.Task

object CountRepository {

  trait Service {
    def appendCurrent(record: InputRecord): Task[Unit]
    def updateHistory(batch: BatchCount): Task[Unit]
    def current(): Task[StateType]
    def history(): Task[List[BatchCount]]
  }

  def apply(countState: CountState) =
    new Service {
      override def appendCurrent(record: InputRecord): Task[Unit] = {
        countState.current.update(_.updatedWith(record.event_type -> record.data)(_.map(_ + 1).orElse(Some(1))))
      }

      override def current(): Task[StateType] = countState.current.get
      override def updateHistory(batch: BatchCount): Task[Unit] = countState.history.update(batch :: _)
      override def history(): Task[List[BatchCount]] = countState.history.get
    }
}
