package org.swabs

import cats.effect.IO
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.comcast.ip4s.Host
import com.comcast.ip4s.IpLiteralSyntax
import com.comcast.ip4s.Port
import com.typesafe.config.{Config => TypeSafeConfig}
import org.swabs.AppConfig.ConfigEntryNotFoundException
import org.swabs.Server.ServerConfig

final class AppConfig(config: TypeSafeConfig) {
  private val serverUrl: IO[String] = IO(config.getString("server.url"))
  private val serverPort: IO[Int] = IO(config.getInt("server.port"))
  private val jwtSecret: IO[String] = IO(config.getString("jwt.secret"))

  val serverConfig: IO[ServerConfig] = {
    (serverUrl.map(Host.fromString), serverPort.map(Port.fromInt)).mapN {
      case (Some(host), Some(port)) => ServerConfig(host, port)
      case _ => ServerConfig(ipv4"0.0.0.0", port"8443")
    }
  }

  val secret: IO[String] = jwtSecret.handleErrorWith(_ => IO.raiseError(ConfigEntryNotFoundException("jwt secret")))
}

object AppConfig {
  final case class ConfigEntryNotFoundException(entity: String) extends Exception {
    override def getMessage: String = s"could not found $entity in config"
  }
}
