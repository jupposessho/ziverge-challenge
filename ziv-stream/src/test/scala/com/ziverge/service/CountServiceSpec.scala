package com.ziverge.service

import com.ziverge.model.{BatchCount, CountResponse, CountState, EventCount, InputRecord, WordCount}
import com.ziverge.model.CountState.StateType
import com.ziverge.repository.CountRepository
import com.ziverge.utils.TestData._
import zio.Ref
import zio.test.Assertion.equalTo
import zio.test._

import scala.collection.immutable.HashMap

object CountServiceSpec extends DefaultRunnableSpec {

  val state = HashMap(("foo", "word") -> 1)
  val emptyHistory = List.empty[BatchCount]

  override def spec = suite("CountService")(
    suite("saveBatch should")(
      testM("add empty batch count to empty history") {
        val expected = List(BatchCount(Nil, fakeNow))
        assertSave(Nil, expected)
      },
      testM("add empty batch count with None element") {
        val expected = List(BatchCount(Nil, fakeNow))
        assertSave(List(None), expected)
      },
      testM("add one element to history") {
        val records = List(Some(InputRecord("foo", "word", 1)))
        val expected = List(BatchCount(List(EventCount("foo", List(WordCount("word", 1)))), fakeNow))

        assertSave(records, expected)
      },
      testM("add history with different event type") {
        val records = List(
          Some(InputRecord("foo", "word", 1)),
          Some(InputRecord("bar", "word", 1))
        )
        val expected = List(
          BatchCount(List(
                       EventCount("bar", List(WordCount("word", 1))),
                       EventCount("foo", List(WordCount("word", 1)))
                     ),
                     fakeNow))

        assertSave(records, expected)
      },
      testM("add history with aggregated elements by event type and word") {
        val records = List(
          Some(InputRecord("foo", "word", 1)),
          Some(InputRecord("foo", "hello", 1)),
          Some(InputRecord("foo", "word", 1))
        )
        val expected =
          List(BatchCount(List(EventCount("foo", List(WordCount("hello", 1), WordCount("word", 2)))), fakeNow))

        assertSave(records, expected)
      },
      testM("add history with complex records, ignoring nulls") {
        val records = List(
          None,
          Some(InputRecord("foo", "word", 1)),
          Some(InputRecord("foo", "hello", 1)),
          Some(InputRecord("foo", "word", 1)),
          None,
          Some(InputRecord("bar", "word", 1)),
          Some(InputRecord("bar", "other", 1)),
          Some(InputRecord("foo", "hello", 1)),
          Some(InputRecord("bar", "word", 1)),
          None
        )
        val expected = List(
          BatchCount(List(
                       EventCount("bar", List(WordCount("word", 2), WordCount("other", 1))),
                       EventCount("foo", List(WordCount("hello", 2), WordCount("word", 2)))
                     ),
                     fakeNow))

        assertSave(records, expected)
      },
      testM("append history to the beginning of the existing one") {
        val history = List(BatchCount(List(EventCount("foo", List(WordCount("bar", 2)))), 1234))
        val records = List(Some(InputRecord("foo", "word", 1)))
        val expected = List(
          BatchCount(List(EventCount("foo", List(WordCount("word", 1)))), fakeNow),
          BatchCount(List(EventCount("foo", List(WordCount("bar", 2)))), 1234)
        )

        assertSave(records, expected, history)
      }
    ),
    suite("count should convert state to count response when")(
      testM("state is empty") {
        assertCount(emptyState, Nil)
      },
      testM("state has one element") {
        val expected = List(EventCount("foo", List(WordCount("word", 1))))
        assertCount(state, expected)
      },
      testM("state has more elements") {
        val state = HashMap(
          ("foo", "word") -> 2,
          ("foo", "hello") -> 2,
          ("bar", "word") -> 2,
          ("bar", "other") -> 1,
        )
        val expected = List(
          EventCount("bar", List(WordCount("word", 2), WordCount("other", 1))),
          EventCount("foo", List(WordCount("hello", 2), WordCount("word", 2)))
        )
        assertCount(state, expected)
      }
    )
  )

  private def assertCount(state: StateType, expected: List[EventCount]) = {
    val result = for {
      (_, service) <- service(state)
      res <- service.count()
    } yield res

    assertM(result)(equalTo(CountResponse(expected)))
  }

  private def assertSave(records: List[Option[InputRecord]],
                         expected: List[BatchCount],
                         history: List[BatchCount] = Nil) = {
    val result = for {
      (repository, service) <- service(emptyState, history)
      _ <- service.saveBatch(records)
      res <- repository.history()
    } yield res
    assertM(result)(equalTo(expected))
  }

  private def service(map: StateType, history: List[BatchCount] = Nil) =
    for {
      cRef <- Ref.make(map)
      hRef <- Ref.make(history)
      countState = CountState(cRef, hRef)
      repository = CountRepository(countState)
    } yield {
      (repository, CountService(repository, fakeClock()))
    }
}
