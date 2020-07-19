package com.ziverge.model

import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

final case class InputRecord(eventType: String, data: String, timestamp: Long)

object InputRecord {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val inputRecordDecoder: Decoder[InputRecord] = deriveConfiguredDecoder
}
