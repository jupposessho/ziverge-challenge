package com.ziverge.routes

import com.ziverge.model.CountState
import com.ziverge.model.CountState.StateType
import com.ziverge.repository.CountRepository
import com.ziverge.service.CountService
import org.http4s.implicits._
import org.http4s.{Method, Request, Status}
import zio.Ref
import zio.interop.catz._
import zio.test._
import zio.test.Assertion.equalTo

import scala.collection.immutable.HashMap

object CountRoutesSpec extends DefaultRunnableSpec {

  val emptyMap = HashMap.empty[String, HashMap[String, Int]]

  override def spec = suite("CountRoutes")(
    suite("GET / should respond with")(
      testM("empty counts") {
        assertCount(emptyMap, """{"counts":[]}""")
      },
      testM("multiple counts") {
        val map = HashMap(
          "foo" -> HashMap("word" -> 2, "hello" -> 2),
          "bar" -> HashMap("word" -> 2, "other" -> 1),
        )
        val expected =
          """{"counts":[
            |{"eventType":"bar","wordCounts":[{"word":"word","count":2},{"word":"other","count":1}]},
            |{"eventType":"foo","wordCounts":[{"word":"hello","count":2},{"word":"word","count":2}]}
            |]}""".stripMargin.replaceAll("\n", "")
        assertCount(map, expected)
      }
    )
  )

  private def assertCount(state: StateType, expected: String) = {
    val result = for {
      routes <- routes(state)
      response <- routes.routes().run(Request(method = Method.GET, uri = uri"/"))
      res <- response.as[String]
    } yield res

    assertM(result)(equalTo(expected))
  }

  private def routes(map: StateType) =
    for {
      ref <- Ref.make(map)
      countState = CountState(ref)
      repository = CountRepository(countState)
      service = CountService(repository)
    } yield CountRoutes(service)
}
