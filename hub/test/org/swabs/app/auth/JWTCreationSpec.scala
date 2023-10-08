package org.swabs.app.auth

import cats.effect.IO
import org.swabs.AppConfig
import org.swabs.TestSpec

import java.time.Clock

class JWTCreationSpec extends TestSpec { self =>
  override val clock: Clock = Clock.systemUTC()

  private class TestSetup(appConfig: AppConfig) extends JwtCreation(appConfig) {
    override protected val clock: Clock = self.clock
  }

  private val appConfig = mock[AppConfig]

  appConfig.secret returns IO.pure("4276301520b7b3d1c6bf22c14ba7a6506a281875")

  "JWTCreation" must {
    "work" in new TestSetup(appConfig) {
      whenReady(createJWT("satoshi"))(_.foreach(_ mustBe a[String]))
    }
  }
}
