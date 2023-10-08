package org.swabs.app

import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.implicits.toSemigroupKOps
import org.http4s._
import org.http4s.dsl._
import org.http4s.play.PlayEntityCodec.playEntityDecoder
import org.http4s.play.PlayEntityCodec.playEntityEncoder
import org.swabs.app.account.services.UserService
import org.swabs.app.auth.JwtAuthenticationMiddleware
import org.swabs.app.geo.models.LookupRadiusRequest
import org.swabs.app.geo.models.UserGeoLocationRequest
import org.swabs.app.geo.services.GeoService
import org.swabs.app.session.models.SignUp
import org.swabs.app.session.services.SignInService
import org.swabs.app.session.services.SignUpService
import org.swabs.core.models.user.User
import org.swabs.core.models.user.UserId

import javax.inject.Inject

final class Routes @Inject()(
    signUpService: SignUpService,
    signInService: SignInService,
    userService: UserService,
    geoService: GeoService,
    middleware: JwtAuthenticationMiddleware
) extends Http4sDsl[IO] {
  lazy val routes: Kleisli[OptionT[IO, *], Request[IO], Response[IO]] = userRoutes <+> sessionRoutes <+> geoRoutes

  private lazy val userRoutes: Kleisli[OptionT[IO, *], Request[IO], Response[IO]] =
    middleware.middleware(getUser) <+>
      middleware.middleware(setUserEvents())

  private lazy val sessionRoutes: Kleisli[OptionT[IO, *], Request[IO], Response[IO]] =
    signUp <+> signIn

  private lazy val geoRoutes: Kleisli[OptionT[IO, *], Request[IO], Response[IO]] =
    middleware.middleware(setUserPosition()) <+>
      middleware.middleware(lookupRadius)

  // todo signature openapi swagger: Base64 encoded signature and pubkey
  private def signUp: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@POST -> Root / "session" / "sign-up" =>
      for {
        signUp <- req.as[SignUp]
        userId <- signUpService.create(signUp)
      } yield Response[IO](Ok).withEntity(userId)
  }

  private def signIn: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@POST -> Root / "session" / "sign-in" =>
      for {
        userId <- req.as[UserId]
        jwt    <- signInService.getJWT(userId)
        resp   <- Ok().map(_.addCookie("jwt", jwt.value))
      } yield resp
  }

  private def getUser: AuthedRoutes[Unit, IO] = AuthedRoutes.of {
    case authReq@POST -> Root / "user" as _ =>
      for {
        userId <- authReq.req.as[UserId]
        user   <- userService.getUser(userId)
      } yield Response[IO](Ok).withEntity(user)
  }

  private def setUserEvents(): AuthedRoutes[Unit, IO] = AuthedRoutes.of {
    case authReq@POST -> Root / "user" / "update-events" as _ =>
      for {
        user <- authReq.req.as[User]
        _    <- userService.setUserEvents(user)
      } yield Response[IO](Created)
  }

  private def setUserPosition(): AuthedRoutes[Unit, IO] = AuthedRoutes.of {
    case authReq@POST -> Root / "user" / "geo" as _ =>
      for {
        geo <- authReq.req.as[UserGeoLocationRequest]
        _   <- geoService.setPosition(geo)
      } yield Response[IO](Created)
  }

  private def lookupRadius: AuthedRoutes[Unit, IO] = AuthedRoutes.of {
    case authReq@POST -> Root / "user" / "geo" / "radius" as _ =>
      for {
        req  <- authReq.req.as[LookupRadiusRequest]
        json <- geoService.lookupRadius(req)
      } yield Response[IO](Ok).withEntity(json)
  }
}
