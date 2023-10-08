package org.swabs.app.session.services

import cats.effect.IO
import cats.implicits.toShow
import org.swabs.AppConfig
import org.swabs.TestSpec
import org.swabs.core.models.user.UserId
import org.swabs.core.redis.{Client => RedisClient}
import org.swabs.util.GlobalDateTimeFormat

import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class SignInServiceSpec extends TestSpec {
  private val now = LocalDateTime.now(Clock.systemUTC()).format(GlobalDateTimeFormat.apply)
  private val userId = UserId(UUID.fromString("f042f433-496f-484e-958f-b8cdd77e622f"))
  private val jsonStr = s"""{"userId":"f042f433-496f-484e-958f-b8cdd77e622f","events":{"signUp":"$now","transactions":[{"dateTime":"$now","money":{"currency":"SATS","amount":123},"note":"satoshi nakamoto is a genius"}]}}"""

  "SignInService#getJWT" must {
    "work" in {
      val redisClient = mock[RedisClient]
      val appConfig = mock[AppConfig]

      appConfig.secret returns IO.pure("4276301520b7b3d1c6bf22c14ba7a6506a281875")
      redisClient.lookup(redisUsersHashCode, userId.show).returns(IO.pure(jsonStr))

      val service = new SignInService(redisClient, appConfig)
      whenReady(service.getJWT(userId))(_.isRight mustBe true)
    }
  }
}
