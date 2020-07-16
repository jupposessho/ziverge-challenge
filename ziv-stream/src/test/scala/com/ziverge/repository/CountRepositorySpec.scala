package com.ziverge.repository

import com.ziverge.model.{BatchCount, CountState, EventCount, WordCount}
import com.ziverge.model.CountState.StateType
import com.ziverge.utils.TestData.emptyState
import zio.Ref
import zio.test._
import zio.test.Assertion._

import scala.collection.immutable.HashMap

object CountRepositorySpec extends DefaultRunnableSpec {

  val state = HashMap(("foo" , "word") -> 1)
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
    suite("setCurrent should")(
      testM("set given state when the current state is empty") {
        val result = for {
          repository <- repository(emptyState)
          _ <- repository.setCurrent(state)
          res <- repository.current()
        } yield res
        assertM(result)(equalTo(state))
      },
      testM("overwrite the current state with given one") {
        val newMap = HashMap(("bar" , "baz") -> 2)
        val result = for {
          repository <- repository(state)
          _ <- repository.setCurrent(newMap)
          res <- repository.current()
        } yield res
        assertM(result)(equalTo(newMap))
      }
    ),
    suite("updateHistory should")(
      testM("set the given state as a history when history is empty") {

        val result = for {
          repository <- repository(emptyState, List.empty[BatchCount])
          _ <- repository.updateHistory(batchCount)
          res <- repository.history()
        } yield res

        assertM(result)(equalTo(List(batchCount)))
      },
      testM("append the given state to the history") {
        val newBatchCount = BatchCount(List(EventCount("fooo", List(WordCount("barrr", 3)))), 2)

        val result = for {
          repository <- repository(emptyState, List(batchCount))
          _ <- repository.updateHistory(newBatchCount)
          res <- repository.history()
        } yield res

        assertM(result)(equalTo(List(newBatchCount, batchCount)))
      }
    )
  )

  private def repository(map: StateType, history: List[BatchCount] = Nil) = for {
    cRef <- Ref.make(map)
    hRef <- Ref.make(history)
    countState = CountState(cRef, hRef)
  } yield CountRepository(countState)
}
