package org.swabs.app.geo.services

import cats.effect.IO
import cats.implicits._
import dev.profunktor.redis4cats.effects.Distance
import dev.profunktor.redis4cats.effects.GeoRadius
import io.lettuce.core.GeoArgs
import org.swabs.app.ServiceEngine
import org.swabs.app.geo.models.GeoUnit._
import org.swabs.app.geo.models.LookupRadiusRequest
import org.swabs.app.geo.models.UserGeoLocationRequest
import org.swabs.app.geo.models.UserGeoRadiusResponse
import org.swabs.core.redis.{Client => RedisClient}

import javax.inject.Inject

final class GeoService @Inject()(client: RedisClient) extends ServiceEngine {
  def setPosition(request: UserGeoLocationRequest): IO[Unit] =
    client.setLocation(redisLocationsHashCode, request.asRedisGeoLocation)

  def lookupRadius(request: LookupRadiusRequest): IO[List[UserGeoRadiusResponse]] =
    for {
      _           <- setPosition(request.userGeoLocation)

      userId      <- IO.pure(request.userGeoLocation.userId.show)

      distance     = Distance(request.distance)
      geoLocation  = request.userGeoLocation.asRedisGeoLocation
      geoRadius    = GeoRadius(geoLocation.lon, geoLocation.lat, distance)

      results     <- client.geoRadius(redisLocationsHashCode, geoRadius, request.unit.toGeoArgsUnit)
      candidates  <- results
                      .filter(_.value.trim != userId.trim)
                      .traverse(c => client.geoDist(redisLocationsHashCode, userId, c.value, GeoArgs.Unit.m).map(c -> _))
                      .map(_.flatMap { case (candidate, distance) => UserGeoRadiusResponse.from(candidate, distance) })
    } yield candidates
}
