package org.swabs.app.session.services

import cats.effect.IO
import cats.implicits.toShow
import org.swabs.AppConfig
import org.swabs.app.ServiceEngine
import org.swabs.app.auth.JwtCreation
import org.swabs.app.auth.models.JwtToken
import org.swabs.core.models.user.UserId
import org.swabs.core.models.user.errors.UserNotFoundException
import org.swabs.core.redis.{Client => RedisClient}

import javax.inject.Inject

final class SignInService @Inject()(client: RedisClient, appConfig: AppConfig) extends JwtCreation(appConfig) with ServiceEngine {
  def getJWT(userToken: UserId): IO[JwtToken] =
    client
      .lookup(redisUsersHashCode, userToken.value.show)
      .handleErrorWith(_ => IO.raiseError(UserNotFoundException(userToken)))
      .*>(createJWT(userToken.value.show).map(JwtToken.apply))
}
