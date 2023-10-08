package org.swabs.app.account.services

import cats.effect.IO
import cats.implicits.showInterpolator
import cats.implicits.toShow
import org.swabs.app.ServiceEngine
import org.swabs.core.models.errors.JsonParsingException
import org.swabs.core.models.user.User
import org.swabs.core.models.user.UserId
import org.swabs.core.models.user.errors.UserNotFoundException
import org.swabs.core.redis.{Client => RedisClient}

import javax.inject.Inject
import scala.util.Try

final class UserService @Inject()(client: RedisClient) extends ServiceEngine {
  // @todo pagination or streaming solution
  def getUser(userId: UserId): IO[User] = {
    for {
      raw  <- client.lookup(redisUsersHashCode, userId.show).handleErrorWith {
                _ => IO.raiseError(UserNotFoundException(userId))
              }
      user <- IO.fromTry(Try(User.parse(raw))).handleErrorWith {
                _ => IO.raiseError(JsonParsingException(show"json of $userId"))
              }
    } yield user
  }

  def setUserEvents(user: User): IO[Unit] =
    for {
      found       <- getUser(user.userId)
      updatedUser  = found.update(user.events)
      _           <- client.update(redisUsersHashCode, updatedUser.userId.show, updatedUser.asJsonString)
    } yield ()
}
