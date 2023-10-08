package org.swabs.app.account.services

import cats.effect.IO
import cats.implicits.showInterpolator
import cats.implicits.toShow
import org.swabs.TestSpec
import org.swabs.core.models.errors.JsonParsingException
import org.swabs.core.models.money.Money.SATS
import org.swabs.core.models.user.User
import org.swabs.core.models.user.UserId
import org.swabs.core.models.user.errors.UserNotFoundException
import org.swabs.core.models.user.events.Events
import org.swabs.core.models.user.events.SignUp
import org.swabs.core.models.user.events.Transactions.Note
import org.swabs.core.models.user.events.Transactions.Transaction
import org.swabs.core.models.user.events.Transactions.TransactionDateTime
import org.swabs.core.redis.{Client => RedisClient}
import org.swabs.util.GlobalDateTimeFormat

import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class UserServiceSpec extends TestSpec {
  private val now = LocalDateTime.now(Clock.systemUTC()).format(GlobalDateTimeFormat.apply)
  private val userId = UserId(UUID.fromString("f042f433-496f-484e-958f-b8cdd77e622f"))
  private val jsonStr = s"""{"userId":"f042f433-496f-484e-958f-b8cdd77e622f","events":{"signUp":"$now","transactions":[{"dateTime":"$now","money":{"currency":"SATS","amount":123},"note":"satoshi nakamoto is a genius"}]}}"""
  private val jsonUpdatedStr = s"""{"userId":"f042f433-496f-484e-958f-b8cdd77e622f","events":{"signUp":"$now","transactions":[{"dateTime":"$now","money":{"currency":"SATS","amount":123},"note":"satoshi nakamoto is a genius"},{"dateTime":"$now","money":{"currency":"SATS","amount":123},"note":"satoshi nakamoto is a genius"}]}}"""
  private val transactions = List(Transaction(
    dateTime = TransactionDateTime(LocalDateTime.parse(now)),
    money = SATS(123),
    note = Note("satoshi nakamoto is a genius")
  ))

  "UserService#getUser" must {
    "work" in {
      val redisClient = mock[RedisClient]

      redisClient.lookup(redisUsersHashCode, userId.show).returns(IO.pure(jsonStr))

      val service = new UserService(redisClient)
      whenReady(service.getUser(userId))(_ mustBe Right(
        User(userId, Events(SignUp(now), transactions))
      ))
    }
    "fail on user not found" in {
      val redisClient = mock[RedisClient]

      redisClient.lookup(redisUsersHashCode, userId.show).returns(IO.raiseError(UserNotFoundException(userId)))

      val service = new UserService(redisClient)
      whenReady(service.getUser(userId))(_ mustBe Left(
        UserNotFoundException(userId)
      ))
    }
    "fail on json parsing error" in {
      val redisClient = mock[RedisClient]

      redisClient.lookup(redisUsersHashCode, userId.show).returns(IO.pure("no json"))

      val service = new UserService(redisClient)
      whenReady(service.getUser(userId))(_ mustBe Left(
        JsonParsingException(show"json of $userId")
      ))
    }
  }

  "UserService#setUserEvents" must {
    "work" in {
      val redisClient = mock[RedisClient]

      redisClient.lookup(redisUsersHashCode, userId.show).returns(IO.pure(jsonStr))
      redisClient.update(redisUsersHashCode, userId.show, jsonUpdatedStr).returns(IO.unit)

      val service = new UserService(redisClient)
      whenReady(service.setUserEvents(User(userId, Events(SignUp(now), transactions))))(_ mustBe Right(()))
    }
  }
}
