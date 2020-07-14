package com.ziverge.repository

import com.ziverge.model.CountState
import com.ziverge.model.CountState.StateType
import zio.Ref
import zio.test._
import zio.test.Assertion._

import scala.collection.immutable.HashMap

object CountRepositorySpec extends DefaultRunnableSpec {

  val emptyMap = HashMap.empty[String, HashMap[String, Int]]
  val map = HashMap("foo" -> HashMap("word" -> 1))

  override def spec = suite("CountRepository")(
    suite("get should")(
      testM("return empty map when no record found") {
        val result = for {
          repository <- repository(emptyMap)
          res <- repository.get()
        } yield res
        assertM(result)(equalTo(emptyMap))
      },
      testM("return the map when not empty") {
        val result = for {
          repository <- repository(map)
          res <- repository.get()
        } yield res
        assertM(result)(equalTo(map))
      },
      testM("react modifications from the outer world") {
        val result = for {
          ref <- Ref.make(map)
          countState = CountState(ref)
          _ <- ref.set(emptyMap)
          res <- CountRepository(countState).get()
        } yield res
        assertM(result)(equalTo(emptyMap))
      }
    ),
    suite("set should")(
      testM("set the empty state with given map") {
        val result = for {
          repository <- repository(emptyMap)
          _ <- repository.set(map)
          res <- repository.get()
        } yield res
        assertM(result)(equalTo(map))
      },
      testM("overwrite the current state with given map") {
        val newMap = HashMap("bar" -> HashMap("baz" -> 2))
        val result = for {
          repository <- repository(map)
          _ <- repository.set(newMap)
          res <- repository.get()
        } yield res
        assertM(result)(equalTo(newMap))
      }
    )
  )

  private def repository(map: StateType) = for {
    ref <- Ref.make(map)
    countState = CountState(ref)
  } yield CountRepository(countState)
}
