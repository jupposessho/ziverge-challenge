package com.ziverge.model

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class CountResponse(counts: List[BatchCount])

object CountResponse {
  implicit val countResponseEncoder: Encoder[CountResponse] = deriveEncoder[CountResponse]
  implicit def countResponseEntityEncoder[F[_]]: EntityEncoder[F, CountResponse] = jsonEncoderOf[F, CountResponse]
}
