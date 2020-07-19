package com.ziverge.routes

import com.ziverge.model.{BatchCount, CountState}
import com.ziverge.model.CountState.StateType
import com.ziverge.repository.CountRepository
import com.ziverge.service.CountService
import com.ziverge.utils.TestData._
import org.http4s.implicits._
import org.http4s.{Method, Request}
import scala.collection.immutable.HashMap
import zio.Ref
import zio.interop.catz._
import zio.test._
import zio.test.Assertion.equalTo

object CountRoutesSpec extends DefaultRunnableSpec {

  override def spec = suite("CountRoutes")(
    suite("GET / should respond with")(
      testM("empty counts") {
        assertM(runCount(emptyState))(equalTo(s"""{"counts":[]}"""))
      },
      testM("multiple events") {
        val currentState = HashMap(
          ("foo", "word") -> 2,
          ("foo", "hello") -> 2
        )
        val expectedV1 =
          s"""{"counts":[{"batch":[
            |{"eventType":"foo","wordCounts":[{"word":"hello","count":2},{"word":"word","count":2}]}
            |],"timestamp":$fakeNow}]}""".stripMargin.replaceAll("\n", "")
        val expectedV2 =
          s"""{"counts":[{"batch":[
            |{"eventType":"foo","wordCounts":[{"word":"word","count":2},{"word":"hello","count":2}]}
            |],"timestamp":$fakeNow}]}""".stripMargin.replaceAll("\n", "")

        assertM(runCount(currentState))(equalTo(expectedV1) || equalTo(expectedV2))
      },
      testM("multiple batches") {
        val currentState = HashMap(
          ("foo", "word") -> 2,
          ("bar", "other") -> 1
        )

        val expectedV1 =
          s"""{"counts":[{"batch":[
            |{"eventType":"bar","wordCounts":[{"word":"other","count":1}]},
            |{"eventType":"foo","wordCounts":[{"word":"word","count":2}]}
            |],"timestamp":$fakeNow}]}""".stripMargin.replaceAll("\n", "")
        val expectedV2 =
          s"""{"counts":[{"batch":[
             |{"eventType":"foo","wordCounts":[{"word":"word","count":2}]},
             |{"eventType":"bar","wordCounts":[{"word":"other","count":1}]}
             |],"timestamp":$fakeNow}]}""".stripMargin.replaceAll("\n", "")

        assertM(runCount(currentState))(equalTo(expectedV1) || equalTo(expectedV2))
      }
    )
  )

  private def runCount[A](currentState: StateType) = {
    for {
      routes <- routes(currentState)
      response <- routes.routes().run(Request(method = Method.GET, uri = uri"/"))
      result <- response.as[String]
    } yield result
  }

  private def routes(currentState: StateType, history: List[BatchCount] = Nil) =
    for {
      cRef <- Ref.make(currentState)
      hRef <- Ref.make(history)
      countState = CountState(cRef, hRef)
      repository = CountRepository(countState)
      service = CountService(repository, fakeClock())
    } yield CountRoutes(service)
}
