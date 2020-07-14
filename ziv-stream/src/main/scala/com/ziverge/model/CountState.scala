package com.ziverge.model

import zio.Ref
import scala.collection.immutable.HashMap
import CountState._

final case class CountState(ref: Ref[StateType]) extends AnyVal

object CountState {

  type StateType = HashMap[String, HashMap[String, Int]]
}
