package com.ziverge.service

import com.ziverge.model.{CountResponse, CountState, EventCount, InputRecord, WordCount}
import com.ziverge.model.CountState.StateType
import com.ziverge.repository.CountRepository
import zio.Ref
import zio.test.Assertion.equalTo
import zio.test._

import scala.collection.immutable.HashMap

object CountServiceSpec extends DefaultRunnableSpec {

  val emptyMap = HashMap.empty[(String, String), Int]
  val map = HashMap(("foo","word") -> 1)

  override def spec = suite("CountService")(
    suite("save should")(
      testM("set empty state") {
        assertSave(Nil, emptyMap, map)
      },
      testM("set empty state with None element") {
        assertSave(List(None), emptyMap, map)
      },
      testM("set state with one element") {
        assertSave(List(Some(InputRecord("foo", "word", 1))), map)
      },
      testM("set state with different event type") {
        val records = List(
          Some(InputRecord("foo", "word", 1)),
          Some(InputRecord("bar", "word", 1)),
        )
        val expected = HashMap(
          ("foo", "word") -> 1,
          ("bar", "word") -> 1
        )

        assertSave(records, expected)
      },
      testM("set state with aggregated elements by event type and word") {
        val records = List(
          Some(InputRecord("foo", "word", 1)),
          Some(InputRecord("foo", "hello", 1)),
          Some(InputRecord("foo", "word", 1)),
        )
        val expected = HashMap(
          ("foo", "word") -> 2, ("foo", "hello") -> 1
        )

        assertSave(records, expected)
      },
      testM("set state with complex records") {
        val records = List(
          Some(InputRecord("foo", "word", 1)),
          Some(InputRecord("foo", "hello", 1)),
          Some(InputRecord("foo", "word", 1)),
          Some(InputRecord("bar", "word", 1)),
          Some(InputRecord("bar", "other", 1)),
          Some(InputRecord("foo", "hello", 1)),
          Some(InputRecord("bar", "word", 1)),
        )
        val expected = HashMap(
          ("foo", "word") -> 2, ("foo", "hello") -> 2,
          ("bar", "word") -> 2, ("bar", "other") -> 1
        )

        assertSave(records, expected)
      }
    ),
    suite("count should convert state to count response when")(
      testM("state is empty") {
        assertCount(emptyMap, Nil)
      },
      testM("state has one element") {
        val expected = List(EventCount("foo", List(WordCount("word", 1))))
        assertCount(map, expected)
      },
      testM("state has more elements") {
        val state = HashMap(
          ("foo", "word") -> 2, ("foo", "hello") -> 2,
          ("bar", "word") -> 2, ("bar", "other") -> 1,
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
                         expected: StateType,
                         initialState: StateType = emptyMap) = {
    val result = for {
      (repository, service) <- service(initialState)
      _ <- service.save(records)
      res <- repository.get()
    } yield res
    assertM(result)(equalTo(expected))
  }

  private def service(map: StateType) =
    for {
      ref <- Ref.make(map)
      countState = CountState(ref)
      repository = CountRepository(countState)
    } yield {
      (repository, CountService(repository))
    }
}
