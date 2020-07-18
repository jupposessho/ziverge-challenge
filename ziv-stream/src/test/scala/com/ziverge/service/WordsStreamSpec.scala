package com.ziverge.service

import com.ziverge.config.Configuration.StreamConfig
import com.ziverge.model.{BatchCount, CountResponse, CountState, EventCount, WordCount}
import com.ziverge.repository.CountRepository
import com.ziverge.utils.TestData._
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import scala.collection.immutable.HashMap
import zio.duration._
import zio.Ref
import zio.test._
import zio.test.Assertion.equalTo

object WordsStreamSpec extends DefaultRunnableSpec {

  val batchCount = BatchCount(List(EventCount("foo", List(WordCount("hello", 1)))), fakeNow)

  override def spec = suite("WordsStream")(
    suite("stream should")(
      testM("persist actual state for one element") {
        val input = """{"event_type":"foo","data":"hello","timestamp":1}"""
        val expected = List(batchCount)

        assertCounts(input, 3, expected)
      },
      testM("produce empty batch if only invalid elements in the bacth") {
        val input = """{"event_type":"foo","data":"hello","timestamp":1}
        ....
        {"event_type":"foo","data":"hello","timestamp":1}"""
        val expected = List(batchCount, BatchCount(List(), fakeNow), batchCount)

        assertCounts(input, 1, expected)
      }
    )
  )

  private def assertCounts(exampleString: String, batchSize: Long, expected: List[BatchCount]) = {
    val input = new ByteArrayInputStream(exampleString.getBytes(StandardCharsets.UTF_8))
    val result = for {
      (service, streamService) <- wordStream(batchSize)
      _ <- streamService.stream(input)
      counts <- service.counts()
    } yield counts

    assertM(result)(equalTo(CountResponse(expected)))
  }

  private def wordStream(batchSize: Long) =
    for {
      cRef <- Ref.make(HashMap.empty[(String, String), Int])
      hRef <- Ref.make(List.empty[BatchCount])
      countState = CountState(cRef, hRef)
      repository = CountRepository(countState)
      service = CountService(repository, fakeClock())
    } yield (service, WordsStream(StreamConfig(10.seconds, batchSize), service))

}
