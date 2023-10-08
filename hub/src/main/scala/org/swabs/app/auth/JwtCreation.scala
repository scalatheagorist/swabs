package org.swabs.app.auth

import cats.effect.IO
import org.swabs.AppConfig
import pdi.jwt.Jwt
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtClaim

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

abstract class JwtCreation(appConfig: AppConfig) {
  private final val hour = 3600
  protected val clock: Clock

  private def toUTCSec(ldt: LocalDateTime): Long = ldt.toEpochSecond(ZoneOffset.UTC)

  def createJWT(subject: String): IO[String] =
    for {
      issuedAt   <- IO(LocalDateTime.now(clock))
      expiration  = issuedAt.plusSeconds(hour)
      claim = JwtClaim(
        subject    = Some(subject),
        issuedAt   = Some(toUTCSec(issuedAt)),
        expiration = Some(toUTCSec(expiration))
      )
      jwt        <- appConfig.secret.map(Jwt.encode(claim, _, JwtAlgorithm.HS256))
    } yield jwt
}
