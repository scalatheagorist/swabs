package org.swabs.app.session.services

import cats.effect.IO
import cats.implicits.toShow
import org.swabs.app.ServiceEngine
import org.swabs.app.session.models.SignUp
import org.swabs.app.session.models.SignUpVerifyException
import org.swabs.core.models.user.User
import org.swabs.core.models.user.UserId
import org.swabs.core.models.user.events.Events
import org.swabs.core.models.user.events.{SignUp => CoreSignUp}
import org.swabs.core.redis.{Client => RedisClient}
import org.swabs.util.SignatureWithPubkey
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger => CatsLogger}

import java.util.UUID
import javax.inject.Inject

final class SignUpService @Inject()(client: RedisClient) extends ServiceEngine {
  private implicit val loggerName: LoggerName = LoggerName(classOf[SignUpService].getName)
  private val logger: CatsLogger[IO] = Slf4jLogger.getLogger[IO]

  def create(signUp: SignUp): IO[UserId] =
    (for {
      isVerified <- IO(SignatureWithPubkey.verify(signUp.signature, signUp.publicKey))
      _          <- IO.unlessA(isVerified)(IO.raiseError(SignUpVerifyException()))

      userId     <- createUUID(client)

      user        = User(userId = userId, events = Events(CoreSignUp.fromClock, Nil))
      userStr    <- IO(user.asJsonString)

      _          <- client.signup(redisUsersHashCode, userId.show, userStr)
    } yield userId)
      .handleErrorWith(ex =>logger.error(ex)(ex.getMessage) *> IO.raiseError(ex))

  private def createUUID(client: RedisClient): IO[UserId] =
    for {
      uuid   <- IO(UUID.randomUUID())
      userId  = UserId(uuid)
      unique <- client.lookup(redisUsersHashCode, userId.show).attempt.flatMap {
                  case Right(_) => createUUID(client)
                  case Left(_)  => IO.pure(userId)
                }
    } yield unique
}
