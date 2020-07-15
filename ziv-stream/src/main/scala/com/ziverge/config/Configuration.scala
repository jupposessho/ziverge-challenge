package com.ziverge.config

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.generic.auto._
import scala.concurrent.duration.FiniteDuration
import zio._
import zio.duration.Duration

object Configuration {

  type Configuration = Has[StreamConfig] with Has[ServerConfig]

  implicit val zioDurationReader = ConfigReader[FiniteDuration].map(Duration.fromScala)

  final case class ServerConfig(host: String, port: Int)

  final case class StreamConfig(interval: Duration, batchSize: Int)

  final case class AppConfig(server: ServerConfig, streamConfig: StreamConfig)

  def load() =
    Task
      .effect(ConfigSource.default.loadOrThrow[AppConfig])
      .toManaged_
}
