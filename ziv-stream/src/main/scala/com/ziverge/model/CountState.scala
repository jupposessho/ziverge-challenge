package com.ziverge.model

import zio.Ref
import scala.collection.immutable.HashMap
import CountState._

final case class CountState(current: Ref[StateType], history: Ref[List[BatchCount]])

object CountState {

  type StateType = HashMap[(String, String), Int]

  val empty = HashMap.empty[(String, String), Int]
}
