package org.swabs.app.auth

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import org.http4s._
import org.http4s.headers.Cookie
import org.http4s.server.AuthMiddleware
import org.swabs.AppConfig
import org.swabs.app.auth.models.CookieNotFoundException
import org.swabs.app.auth.models.JwtDecodingException
import pdi.jwt.Jwt
import pdi.jwt.JwtAlgorithm

import javax.inject.Inject
import scala.util.Success

final class JwtAuthenticationMiddleware @Inject()(appConfig: AppConfig) {
  lazy val middleware: AuthMiddleware[IO, Unit] = AuthMiddleware(authUser)

  private val authUser: Kleisli[OptionT[IO, *], Request[IO], Unit] =
    Kleisli(request => OptionT.liftF {
      val cookieContent = for {
        header  <- request.headers.get[Cookie]
        content <- header.values.toList.find(_.name == "jwt").map(_.content)
      } yield content

      IO
        .fromOption(cookieContent)(CookieNotFoundException("cookie (jwt) was not found in the request header"))
        .flatMap(decodeJwt)
    })

  private def decodeJwt(cookie: String): IO[Unit] =
    appConfig.secret.flatMap { secret =>
      Jwt.decodeRawAll(cookie, secret, JwtAlgorithm.HS256 :: Nil) match {
        case Success(_) if Jwt.isValid(cookie, secret, Seq(JwtAlgorithm.HS256)) =>
          IO.unit
        case _ =>
          IO.raiseError(JwtDecodingException)
      }
    }
}
