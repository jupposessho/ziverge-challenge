package com.ziverge.service

import com.ziverge.config.Configuration.ServerConfig
import java.io.{File, IOException, InputStream}
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.blocking._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Bootstrap {

  def server(config: ServerConfig, routes: HttpApp[Task]) =
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit rts =>
        BlazeServerBuilder[Task]
          .bindHttp(config.port, config.host)
          .withHttpApp(routes)
          .serve
          .compile
          .drain
      }

  def open(file: String) = {
    val acquire = effectBlocking(Tail.follow(new File(file))).refineToOrDie[IOException]
    val release = (input: InputStream) => effectBlocking(input.close()).orDie

    Managed.make(acquire)(release)
  }
}
