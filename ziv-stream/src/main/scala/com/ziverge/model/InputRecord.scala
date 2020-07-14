package com.ziverge.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class InputRecord(event_type: String, data: String, timestamp: Long)

object InputRecord {
  implicit val fooDecoder: Decoder[InputRecord] = deriveDecoder[InputRecord]
}
