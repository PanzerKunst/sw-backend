package services

import play.api.mvc.Headers
import play.Play
import java.util.{Date, UUID}
import db.AccountDto
import play.api.http.HeaderNames
import play.mvc.Http.Status
import models.OauthAccessToken

object AuthHelper {
  var usedNonces: Map[String, Long] = Map() // Map[nonce, timestamp]
  var liveAccessTokens: Map[String, OauthAccessToken] = Map() // Map[clientIdentifier, oauthAccessToken]

  def generateToken(headers: Headers): OauthAccessToken = {
    val authorizationHeader = getAuthorizationHeader(headers).get
    val clientIdentifier = getSubHeader(authorizationHeader, "oauth_consumer_key").get

    // If a token already exists for this clientIdentifier, we revoke it
    revokeTokenForClientIdentifier(clientIdentifier)

    val generatedToken = OauthAccessToken(token = UUID.randomUUID.toString,
      sharedSecret = UUID.randomUUID.toString)

    liveAccessTokens += (clientIdentifier -> generatedToken)

    generatedToken
  }

  def revokeToken(headers: Headers) {
    val authorizationHeader = getAuthorizationHeader(headers).get
    val clientIdentifier = getSubHeader(authorizationHeader, "oauth_consumer_key").get

    revokeTokenForClientIdentifier(clientIdentifier)
  }

  def checkRequestAuthentication(requestMethod: String, uri: String, headers: Headers): Int = {
    if (!isRequestValid(headers))
      Status.BAD_REQUEST
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
      if (timestamp > currentTimestamp ||
        currentTimestamp - timestamp > Play.application().configuration().getString("auth.retainedTimeframeInSeconds").toLong)
        Status.UNAUTHORIZED
      else {

        // Client identifier
        val filters = Some(Map("username" -> clientIdentifier))
        val matchingUsers = AccountDto.get(filters)

        if (matchingUsers.isEmpty)
          Status.UNAUTHORIZED
        else {
          usedNonces.get(nonce) match {
            case Some(usedNonce) => Status.UNAUTHORIZED
            case None =>
              usedNonces += (nonce -> currentTimestamp)

              // Token
              getTokenSharedSecret(clientIdentifier, token) match {
                case None => Status.UNAUTHORIZED
                case Some(tokenSharedSecret) =>

                  // Signature
                  buildSignatureFromRequestData(requestMethod, uri, clientIdentifier, None) match {
                    case None => Status.UNAUTHORIZED
                    case Some(builtSignature) =>
                      if (signature != builtSignature)
                        Status.UNAUTHORIZED
                      else
                        Status.OK
                  }
              }
          }
        }
      }
    }
  }

  def checkTokenRequestAuthentication(requestMethod: String, uri: String, headers: Headers): Int = {
    if (!isTokenRequestValid(headers))
      Status.BAD_REQUEST
    else {
      val authorizationHeader = getAuthorizationHeader(headers).get

      val timestampString = getSubHeader(authorizationHeader, "oauth_timestamp").get
      val clientIdentifier = getSubHeader(authorizationHeader, "oauth_consumer_key").get
      val nonce = getSubHeader(authorizationHeader, "oauth_nonce").get
      val signature = getSubHeader(authorizationHeader, "oauth_signature").get

      // Timestamp
      val timestamp = timestampString.toLong
      val currentTimestamp = new Date().getTime / 1000

      if (timestamp > currentTimestamp ||
        currentTimestamp - timestamp > Play.application().configuration().getString("auth.retainedTimeframeInSeconds").toLong)
        Status.UNAUTHORIZED
      else {

        // Client identifier
        val filters = Some(Map("username" -> clientIdentifier))
        val matchingUsers = AccountDto.get(filters)

        if (matchingUsers.isEmpty)
          Status.UNAUTHORIZED
        else {
          usedNonces.get(nonce) match {
            case Some(usedNonce) => Status.UNAUTHORIZED
            case None =>
              usedNonces += (nonce -> currentTimestamp)

              // Signature
              buildSignatureFromRequestData(requestMethod, uri, clientIdentifier, None) match {
                case None => Status.UNAUTHORIZED
                case Some(builtSignature) =>
                  if (signature != builtSignature)
                    Status.UNAUTHORIZED
                  else
                    Status.OK
              }
          }
        }
      }
    }
  }

  private def revokeTokenForClientIdentifier(clientIdentifier: String) {
    liveAccessTokens -= clientIdentifier
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

  private def isTokenRequestValid(headers: Headers): Boolean = {
    getAuthorizationHeader(headers) match {
      case None => false
      case Some(authorizationHeader) =>
        val realm = getSubHeader(authorizationHeader, "realm")
        val signatureMethod = getSubHeader(authorizationHeader, "oauth_signature_method")
        val timestampString = getSubHeader(authorizationHeader, "oauth_timestamp")
        val clientIdentifier = getSubHeader(authorizationHeader, "oauth_consumer_key")
        val nonce = getSubHeader(authorizationHeader, "oauth_nonce")
        val signature = getSubHeader(authorizationHeader, "oauth_signature")

        if (realm.isDefined && signatureMethod.isDefined && timestampString.isDefined && clientIdentifier.isDefined &&
          nonce.isDefined && signature.isDefined)
          realm.get == Play.application().configuration().getString("auth.header.realm") &&
            signatureMethod.get == Play.application().configuration().getString("auth.header.signatureMethod") &&
            isTimestampHeaderValid(timestampString.get)
        else
          false
    }
  }

  private def isTimestampHeaderValid(timestampString: String): Boolean = {
    try {
      timestampString.toLong
      true
    }
    catch {
      case nfe: NumberFormatException => false
    }
  }

  private def buildSignatureFromRequestData(requestMethod: String, uri: String, clientIdentifier: String, tokenSharedSecret: Option[String]): Option[String] = {
    AccountDto.getPasswordForUsername(clientIdentifier) match {
      case None =>
        None
      case Some(clientSharedSecret) =>
        val text = requestMethod + "&" + uri

        var key = clientSharedSecret + "&"

        if (tokenSharedSecret.isDefined) {
          key = key + tokenSharedSecret.get
        }

        Some(CryptoHelper.hmacSha1(text, key))
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

  private def getAuthorizationHeader(headers: Headers): Option[String] = {
    headers.get(HeaderNames.AUTHORIZATION)
  }

  private def getSubHeader(header: String, key: String): Option[String] = {
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
}
