package services.auth.helper

import play.Play
import play.api.mvc.Headers
import play.api.http.Status
import services.auth.AuthenticationResult

class MockAuthHelper extends AbstractAuthHelper with AuthHelper {
  val authMockClientIdentifier = Play.application().configuration().getString("auth.mockClientIdentifier")

  def checkRequestAuthentication(requestMethod: String, uri: String, headers: Headers): AuthenticationResult = {
      AuthenticationResult(Status.OK)
  }

  def getAuthorizationHeader(headers: Headers): Option[String] = {
      Some("")
  }

  def getSubHeader(header: String, key: String): Option[String] = {
    if (key == "oauth_consumer_key")
      Some(authMockClientIdentifier)
    else None
  }
}
