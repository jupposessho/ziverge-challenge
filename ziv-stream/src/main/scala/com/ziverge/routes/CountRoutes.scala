package com.ziverge.routes

import com.ziverge.service.CountService
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.implicits._
import zio.Task
import zio.interop.catz._
import zio.interop.catz.implicits._

object CountRoutes {

  trait Service {
   def routes(): HttpApp[Task]
  }

  def apply(countService: CountService.Service) = {
    new Service {
      private val dsl = Http4sDsl[Task]

      import dsl._

      def routes(): HttpApp[Task] =
        HttpRoutes
          .of[Task] {
            case GET -> Root =>
              for {
                countResponse <- countService.count()
                resp <- Ok(countResponse)
              } yield resp
          }
          .orNotFound
    }
  }
}
