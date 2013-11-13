package services.auth.helper

import play.api.mvc.Headers
import java.util.Date
import services.auth.AuthenticationResult
import play.mvc.Http.Status
import play.api.Logger
import db.AccountDto
import play.api.http.HeaderNames

class RealAuthHelper extends AbstractAuthHelper with AuthHelper {
  def checkRequestAuthentication(requestMethod: String, uri: String, headers: Headers): AuthenticationResult = {
    if (!isRequestValid(headers))
      AuthenticationResult(Status.BAD_REQUEST)
    else {
      val authorizationHeader = getAuthorizationHeader(headers).get

      val timestampString = getSubHeader(authorizationHeader, "oauth_timestamp").get
      val clientIdentifier = getSubHeader(authorizationHeader, "oauth_consumer_key").get
      val nonce = getSubHeader(authorizationHeader, "oauth_nonce").get
      val signature = getSubHeader(authorizationHeader, "oauth_signature").get
      val token = getSubHeader(authorizationHeader, "oauth_token").get

      // Timestamp
      val timestamp = timestampString.toLong
      val currentTimestamp = new Date().getTime / 1000

      // TODO: remove
      Logger.info("currentTimestamp: " + currentTimestamp)

      if (timestamp > currentTimestamp)
        AuthenticationResult(Status.UNAUTHORIZED, Some("Timestamp cannot be in the future"))
      else if (currentTimestamp - timestamp > authRetainedTimeframeInSeconds)
        AuthenticationResult(Status.UNAUTHORIZED, Some("Timestamp older than the retained timeframe"))
      else {

        // Client identifier
        val filters = Some(Map("username" -> clientIdentifier))
        val matchingUsers = AccountDto.get(filters)

        if (matchingUsers.isEmpty)
          AuthenticationResult(Status.UNAUTHORIZED, Some("Wrong Client Identifier"))
        else {
          usedNonces.get(nonce) match {
            case Some(usedNonce) => AuthenticationResult(Status.UNAUTHORIZED, Some("This nonce was already used"))
            case None =>
              usedNonces += (nonce -> currentTimestamp)

              // Token
              getTokenSharedSecret(clientIdentifier, token) match {
                case None => AuthenticationResult(Status.UNAUTHORIZED, Some("Wrong Token Shared Secret"))
                case Some(tokenSharedSecret) =>

                  // Signature
                  buildSignatureFromRequestData(requestMethod, uri, clientIdentifier, None) match {
                    case None => AuthenticationResult(Status.UNAUTHORIZED, Some("Unable to build signature from authorisation data"))
                    case Some(builtSignature) =>
                      if (signature != builtSignature)
                        AuthenticationResult(Status.UNAUTHORIZED, Some("Wrong signature"))
                      else
                        AuthenticationResult(Status.OK)
                  }
              }
          }
        }
      }
    }
  }

  def getAuthorizationHeader(headers: Headers): Option[String] = {
    headers.get(HeaderNames.AUTHORIZATION)
  }

  def getSubHeader(header: String, key: String): Option[String] = {
    val regex = (".*" + key + """\s*=\s*"([^"]+)".*""").r

    try {
      val regex(result) = header

      if (result.isEmpty)
        None
      else
        Some(result)
    }
    catch {
      case me: MatchError => None
    }
  }

  private def isRequestValid(headers: Headers): Boolean = {
    if (!isTokenRequestValid(headers)) {
      false
    }
    else {
      // A normal request (post-auth) has the same Authorization headers as a token request, plus "oauth_token"
      val authorizationHeader = getAuthorizationHeader(headers).get
      getSubHeader(authorizationHeader, "oauth_token").isDefined
    }
  }

  private def getTokenSharedSecret(clientIdentifier: String, accessToken: String): Option[String] = {
    liveAccessTokens.get(clientIdentifier) match {
      case None => None
      case Some(tokenPair) =>
        if (tokenPair.token != accessToken)
          None
        else
          Some(tokenPair.sharedSecret)
    }
  }
}
