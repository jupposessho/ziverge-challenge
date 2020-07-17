package com.ziverge.model

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class BatchCount(batch: List[EventCount], timestamp: Long)
final case class EventCount(eventType: String, wordCounts: List[WordCount])
final case class WordCount(word: String, count: Int)

object BatchCount {
  implicit val batchCountEncoder: Encoder[BatchCount] = deriveEncoder[BatchCount]
}
object EventCount {
  implicit val wordCountEncoder: Encoder[EventCount] = deriveEncoder[EventCount]
}
object WordCount {
  implicit val wordCountEncoder: Encoder[WordCount] = deriveEncoder[WordCount]
}
