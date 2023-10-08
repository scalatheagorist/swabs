package org.swabs.core.redis

import cats.effect.IO
import cats.effect.Resource
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.effect.Log.Stdout._
import dev.profunktor.redis4cats.effects
import dev.profunktor.redis4cats.effects.GeoLocation
import dev.profunktor.redis4cats.effects.GeoRadius
import io.lettuce.core.GeoArgs
import org.swabs.core.redis.Client.RedisConfig
import org.swabs.core.redis.models.KeyNotFoundException
import org.swabs.core.redis.models.SignupException
import org.swabs.core.redis.models.UpdateException
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger => CatsLogger}

import javax.inject.Inject

class Client @Inject()(config: RedisConfig) {
  private implicit val loggerName: LoggerName = LoggerName(classOf[Client].getName)
  private val logger: CatsLogger[IO] = Slf4jLogger.getLogger[IO]

  private val api: Resource[IO, RedisCommands[IO, String, String]] = Redis[IO].utf8(config.uri)

  def lookup(hashCode: String, key: String): IO[String] =
    api.use { cmd =>
      cmd
        .hGet(hashCode, key)
        .flatMap(IO.fromOption(_)(KeyNotFoundException(key)))
    }

  def geoRadius(hashCode: String, radius: GeoRadius, unit: GeoArgs.Unit): IO[List[effects.GeoRadiusResult[String]]] =
    api.use(_.geoRadius(hashCode, radius, unit, GeoArgs.Builder.coordinates()))

  def geoDist(hashCode: String, from: String, candidate: String, unit: GeoArgs.Unit): IO[Double] =
    api.use(_.geoDist(hashCode, from, candidate, unit))

  def setLocation(hashCode: String, locations: GeoLocation[String]): IO[Unit] =
    api.use(_.geoAdd(hashCode, locations))

  def signup(hashCode: String, key: String, field: String): IO[Unit] =
    (for {
      isEntity <- api.use(_.hGet(hashCode, key))
      _        <- IO.whenA(isEntity.isEmpty)(IO.raiseError(SignupException))
      _        <- update(hashCode, key, field)
    } yield ()).handleErrorWith {
      case ex: UpdateException =>
        logger.error(ex)(SignupException.getMessage) *> IO.raiseError(ex)
      case ex =>
        logger.error(ex)(ex.getMessage) *> IO.raiseError(ex)
    }

  def update(hashCode: String, key: String, field: String): IO[Unit] =
    api.use { cmd =>
      for {
        isUpdated <- cmd.hSet(hashCode, key, field)
        _         <- IO.unlessA(isUpdated) {
                       val message   = s"could not update by key $key"
                       val exception = UpdateException(message)
                       IO.raiseError(exception) <* logger.error(exception)(message)
                     }
      } yield ()
    }
}

object Client {
  final case class RedisConfig(uri: String, port: Option[Int])
}
