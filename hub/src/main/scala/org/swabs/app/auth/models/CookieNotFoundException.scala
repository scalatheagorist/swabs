package org.swabs.app.auth.models

final case class CookieNotFoundException(message: String) extends Exception {
  override def getMessage: String = message
}
