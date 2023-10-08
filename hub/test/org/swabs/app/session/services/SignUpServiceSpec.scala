package org.swabs.app.session.services

import cats.effect.IO
import org.swabs.TestSpec
import org.swabs.app.session.models.SignUp
import org.swabs.core.redis.{Client => RedisClient}

class SignUpServiceSpec extends TestSpec {
  private val signatureBased64 = "MEUCIQCAm1NmIlmmM/w0fVHJM9Y/Bvr8prDKvDw0aap4vnHJdwIgQH8r3QEOvC6j0lL1B2H6godV+BCwm/JAgz/Rq9Ro4MU="
  private val pubKeyBased64 = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEN+t9SVofjw7f4AjZJXnxprf/fMSIYfZDhhCjQCqnq7u6w6Dsz+u0Gb17I80CwZdI+IqZ8u6IgfxxAmbtoLZE/Q=="

  "SignUpService#create" must {
    "work with new SignUp" in {
      val signUp = SignUp(signatureBased64, pubKeyBased64)
      val redisClient = mock[RedisClient]

      redisClient
        .lookup(redisUsersHashCode, *[String])
        .returns(IO.raiseError(new Throwable))

      redisClient.signup(redisUsersHashCode, *[String], *[String]).returns(IO.unit)

      val service = new SignUpService(redisClient)
      whenReady(service.create(signUp))(_.isRight mustBe true)
    }
    "work with new SignUp (2x iterations)" in {
      val signUp = SignUp(signatureBased64, pubKeyBased64)
      val redisClient = mock[RedisClient]

      redisClient
        .lookup(redisUsersHashCode, *[String])
        .returns(IO.pure("d4b1a0f0-dbb8-4aa4-a02d-98a55fbfd604"))

      redisClient
        .lookup(redisUsersHashCode, *[String])
        .returns(IO.raiseError(new Throwable))

      redisClient.signup(redisUsersHashCode, *[String], *[String]).returns(IO.unit)

      val service = new SignUpService(redisClient)
      whenReady(service.create(signUp))(_.isRight mustBe true)
    }
  }
}
