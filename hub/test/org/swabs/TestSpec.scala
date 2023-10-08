package org.swabs

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.mockito.scalatest.IdiomaticMockito
import org.scalatestplus.play.PlaySpec
import org.swabs.app.ServiceEngine

class TestSpec extends PlaySpec with IdiomaticMockito with ServiceEngine {
  private implicit val runtime: IORuntime = IORuntime.global

  def whenReady[T, U](io: => IO[T])(f: Either[Throwable, T] => U): U =
    io.attempt.map(f).unsafeRunSync()
}
