package org.swabs

import com.google.inject.AbstractModule
import com.typesafe.config.ConfigFactory
import net.codingwell.scalaguice.ScalaModule
import org.swabs.app.Routes
import org.swabs.app.account.services.UserService
import org.swabs.app.auth.JwtAuthenticationMiddleware
import org.swabs.app.geo.services.GeoService
import org.swabs.app.session.services.SignInService
import org.swabs.app.session.services.SignUpService
import org.swabs.core.redis.Client.RedisConfig
import org.swabs.core.redis.{Client => RedisClient}

class Module extends AbstractModule with ScalaModule {
  private val config      = ConfigFactory.load("application.conf")
  private val redisConfig = RedisConfig(s"redis://${config.getString("redis.single-node-address")}", None)
  private val appConfig   = new AppConfig(config)

  override def configure(): Unit = {
    bind[AppConfig].toInstance(appConfig)
    bind[JwtAuthenticationMiddleware].asEagerSingleton()

    bind[RedisConfig].toInstance(redisConfig)
    bind[RedisClient].asEagerSingleton()

    bind[SignUpService].asEagerSingleton()
    bind[SignInService].asEagerSingleton()
    bind[UserService].asEagerSingleton()
    bind[GeoService].asEagerSingleton()

    bind[Routes].asEagerSingleton()
  }
}
