package org.swabs.app.auth

import cats.data.NonEmptyList
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.Cookie
import org.http4s.implicits.http4sLiteralsSyntax
import org.swabs.AppConfig
import org.swabs.TestSpec
import org.swabs.app.auth.models.CookieNotFoundException
import org.swabs.app.auth.models.JwtDecodingException

class JwtAuthenticationMiddlewareSpec extends TestSpec {
  private val appConfig = mock[AppConfig]

  appConfig.secret returns IO.pure("4276301520b7b3d1c6bf22c14ba7a6506a281875")

  private val middelware = new JwtAuthenticationMiddleware(appConfig)

  "JwtAuthenticationMiddleware#decodeJWT" must {
    "work" in {
      val response = Response.apply[IO](Status.Ok).withEmptyBody
      val routes = AuthedRoutes.of[Unit, IO] {
        case GET -> Root / "test" as _ => IO.pure(response)
      }
      val cookie = new Cookie(NonEmptyList.of(RequestCookie(
        name = "jwt",
        content = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoic2F0b3NoaSJ9.adEJMdHjZsb0QQiqOsjd_gobrQXDH6TSzQLoCZqOBAE"
      )))
      val requestWithCookie =
        middelware
          .middleware(routes)
          .orNotFound
          .run(Request(method = Method.GET, uri = uri"/test", headers = Headers(cookie)))

      whenReady(requestWithCookie)(_ mustBe Right(response))
    }
    "failed in case of missing cookie" in {
      val routes = AuthedRoutes.of[Unit, IO] {
        case GET -> Root / "test" as _ => IO.pure(Response.apply[IO](Status.InternalServerError))
      }
      val requestWithCookie =
        middelware
          .middleware(routes)
          .orNotFound
          .run(Request(method = Method.GET, uri = uri"/test", headers = Headers(Nil)))

      whenReady(requestWithCookie)(_ mustBe Left(CookieNotFoundException("cookie (jwt) was not found in the request header")))
    }
    "failed in case of invalid jwt" in {
      val routes = AuthedRoutes.of[Unit, IO] {
        case GET -> Root / "test" as _ => IO.pure(Response.apply[IO](Status.InternalServerError))
      }
      val cookie = new Cookie(NonEmptyList.of(RequestCookie(name = "jwt", content = "invalid cookie")))
      val requestWithCookie =
        middelware
          .middleware(routes)
          .orNotFound
          .run(Request(method = Method.GET, uri = uri"/test", headers = Headers(cookie)))

      whenReady(requestWithCookie)(_ mustBe Left(JwtDecodingException))
    }
  }
}
