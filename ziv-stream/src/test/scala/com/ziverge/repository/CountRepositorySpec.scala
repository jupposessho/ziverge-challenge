package com.ziverge.repository

import com.ziverge.model.{BatchCount, CountState, EventCount, WordCount}
import com.ziverge.model.CountState.StateType
import com.ziverge.utils.TestData.emptyState
import zio.Ref
import zio.test._
import zio.test.Assertion._

import scala.collection.immutable.HashMap
import com.ziverge.model.InputRecord

object CountRepositorySpec extends DefaultRunnableSpec {

  val state = HashMap(("foo", "word") -> 1)
  val batchCount = BatchCount(List(EventCount("foo", List(WordCount("bar", 2)))), 1)

  override def spec = suite("CountRepository")(
    suite("get should")(
      testM("return empty map when no record found") {
        val result = for {
          repository <- repository(emptyState)
          res <- repository.current()
        } yield res
        assertM(result)(equalTo(emptyState))
      },
      testM("return the map when not empty") {
        val result = for {
          repository <- repository(state)
          res <- repository.current()
        } yield res

        assertM(result)(equalTo(state))
      },
      testM("react modifications from the outer world") {
        val result = for {
          cRef <- Ref.make(state)
          hRef <- Ref.make(List.empty[BatchCount])
          countState = CountState(cRef, hRef)
          _ <- cRef.set(emptyState)
          res <- CountRepository(countState).current()
        } yield res

        assertM(result)(equalTo(emptyState))
      }
    ),
    suite("appendCurrent should append the current record to the existing state when")(
      testM("current state is empty") {
        val record = InputRecord("foo", "word", 1)
        assertAppend(emptyState, record, state)
      },
      testM("event type and word matches") {
        val record = InputRecord("foo", "word", 1)
        val expected = HashMap(("foo", "word") -> 2)

        assertAppend(state, record, expected)
      },
      testM("event type is different") {
        val record = InputRecord("bar", "word", 1)
        val expected = HashMap(("foo", "word") -> 1, ("bar", "word") -> 1)

        assertAppend(state, record, expected)
      },
      testM("word is different") {
        val record = InputRecord("foo", "baz", 1)
        val expected = HashMap(("foo", "word") -> 1, ("foo", "baz") -> 1)

        assertAppend(state, record, expected)
      }
    ),
    suite("updateHistory should")(
      testM("set the given state as a history when history is empty") {
        assertUpdateHistory(emptyState, Nil, batchCount, List(batchCount))
      },
      testM("append the given state to the history") {
        val newBatchCount = BatchCount(List(EventCount("fooo", List(WordCount("barrr", 3)))), 2)
        assertUpdateHistory(emptyState, List(batchCount), newBatchCount, List(newBatchCount, batchCount))
      }
    )
  )

  private def assertUpdateHistory(initialState: StateType,
                                  history: List[BatchCount],
                                  newBatchCount: BatchCount,
                                  expected: List[BatchCount]) = {
    val result = for {
      repository <- repository(emptyState, history)
      _ <- repository.updateHistory(newBatchCount)
      res <- repository.history()
    } yield res

    assertM(result)(equalTo(expected))
  }

  private def assertAppend(initialState: StateType, record: InputRecord, expected: StateType) = {
    val result = for {
      repository <- repository(initialState)
      _ <- repository.appendCurrent(record)
      res <- repository.current()
    } yield res
    assertM(result)(equalTo(expected))
  }

  private def repository(map: StateType, history: List[BatchCount] = Nil) =
    for {
      cRef <- Ref.make(map)
      hRef <- Ref.make(history)
      countState = CountState(cRef, hRef)
    } yield CountRepository(countState)
}
