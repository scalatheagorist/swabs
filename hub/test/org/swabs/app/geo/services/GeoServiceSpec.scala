package org.swabs.app.geo.services

import cats.effect.IO
import dev.profunktor.redis4cats.effects.Distance
import dev.profunktor.redis4cats.effects.GeoCoordinate
import dev.profunktor.redis4cats.effects.GeoHash
import dev.profunktor.redis4cats.effects.GeoLocation
import dev.profunktor.redis4cats.effects.GeoRadius
import dev.profunktor.redis4cats.effects.GeoRadiusResult
import dev.profunktor.redis4cats.effects.Latitude
import dev.profunktor.redis4cats.effects.Longitude
import io.lettuce.core.GeoArgs
import org.swabs.TestSpec
import org.swabs.app.geo.models.Coordinate
import org.swabs.app.geo.models.GeoUnit
import org.swabs.app.geo.models.LookupRadiusRequest
import org.swabs.app.geo.models.UserGeoLocationRequest
import org.swabs.app.geo.models.UserGeoRadiusResponse
import org.swabs.core.models.user.UserId
import org.swabs.core.redis.{Client => RedisClient}

import java.util.UUID

class GeoServiceSpec extends TestSpec {
  "GeoService#setPosition" must {
    "work" in {
      val redisClient = mock[RedisClient]
      val uuid = "d4b1a0f0-dbb8-4aa4-a02d-98a55fbfd604"
      val geoLocation = GeoLocation[String](Longitude(59.90975646224532), Latitude(10.740845013978463), uuid)

      redisClient.setLocation(redisLocationsHashCode, geoLocation) returns IO.unit

      val service = new GeoService(redisClient)
      whenReady {
        service.setPosition(UserGeoLocationRequest(Coordinate(59.90975646224532, 10.740845013978463), UserId(UUID.fromString(uuid))))
      }(_ mustBe Right(()))
    }
  }

  "GeoService#lookupRadius" must {
    "work" in {
      val redisClient = mock[RedisClient]
      val uuid = "d4b1a0f0-dbb8-4aa4-a02d-98a55fbfd604"

      val service = new GeoService(redisClient)
      val nextUser1UUID = "e201c85d-4f54-44f6-9fae-8678bedc97b4"
      val user = GeoLocation(Longitude(59.91400139002258), Latitude(10.745020273403547), uuid)

      redisClient.setLocation(redisLocationsHashCode, user) returns IO.unit
      redisClient
        .geoRadius(
          hashCode = redisLocationsHashCode,
          radius = GeoRadius(
            Longitude(59.91400139002258),
            Latitude(10.745020273403547),
            Distance(1000)
          ),
          unit = GeoArgs.Unit.m
        )
        .returns(IO.pure(List(
          GeoRadiusResult(
            value = nextUser1UUID,
            dist = Distance(600),
            hash = GeoHash(0),
            coordinate = GeoCoordinate(59.90975646224532, 10.740845013978463)
          )
        )))
      redisClient.geoDist(redisLocationsHashCode, uuid, nextUser1UUID, GeoArgs.Unit.m) returns IO.pure(600.0)

      val program0: IO[List[UserGeoRadiusResponse]] = service.lookupRadius(
        request = LookupRadiusRequest(UserGeoLocationRequest(Coordinate(59.91400139002258, 10.745020273403547),
          userId = UserId(UUID.fromString(uuid))),
          distance = 1000.0,
          unit = GeoUnit.M
        )
      )

      whenReady(program0)(_ mustBe Right(List(
        UserGeoRadiusResponse(
          userId = UserId(UUID.fromString(nextUser1UUID)),
          dist = 600,
          coordinate = Coordinate(59.90975646224532, 10.740845013978463)
        )
      )))
    }
  }
}
