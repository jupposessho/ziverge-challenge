package com.ziverge.repository

import com.ziverge.model.{BatchCount, CountState}
import com.ziverge.model.CountState.StateType
import zio.Task

object CountRepository {

  trait Service {
    def setCurrent(state: StateType): Task[Unit]
    def updateHistory(batch: BatchCount): Task[Unit]
    def current(): Task[StateType]
    def history(): Task[List[BatchCount]]
  }

  def apply(countState: CountState) =
    new Service {
      override def setCurrent(state: StateType): Task[Unit] = countState.current.setAsync(state)
      override def current(): Task[StateType] = countState.current.get
      override def updateHistory(batch: BatchCount): Task[Unit] = countState.history.update(batch :: _)
      override def history(): Task[List[BatchCount]] = countState.history.get
    }
}


