package com.ziverge.model

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class CountResponse(counts: List[EventCount])
final case class EventCount(eventType: String, wordCounts: List[WordCount])
final case class WordCount(word: String, count: Long)

object CountResponse {
  implicit val countResponseEncoder: Encoder[CountResponse] = deriveEncoder[CountResponse]
  implicit def countResponseEntityEncoder[F[_]]: EntityEncoder[F, CountResponse] = jsonEncoderOf[F, CountResponse]
}
object EventCount {
  implicit val wordCountEncoder: Encoder[EventCount] = deriveEncoder[EventCount]
}
object WordCount {
  implicit val wordCountEncoder: Encoder[WordCount] = deriveEncoder[WordCount]
}
