package org.swabs.app

import java.time.Clock

trait ServiceEngine {
  implicit val clock: Clock = Clock.systemUTC()
  final val redisUsersHashCode: String = "users"
  final val redisLocationsHashCode: String = "locations"
}
