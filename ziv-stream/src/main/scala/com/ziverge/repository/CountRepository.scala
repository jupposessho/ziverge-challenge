package com.ziverge.repository

import com.ziverge.model.CountState
import com.ziverge.model.CountState.StateType
import zio.Task

object CountRepository {

  trait Service {
    def set(value: StateType): Task[Unit]
    def get(): Task[StateType]
  }

  def apply(countState: CountState) =
    new Service {
      override def set(value: StateType): Task[Unit] = countState.ref.setAsync(value)
      override def get(): Task[StateType] = countState.ref.get
    }
}


