package services.auth.helper

import play.Play
import play.api.mvc.Headers
import services.auth.AuthenticationResult
import models.OauthAccessToken
import java.util.{Date, UUID}
import play.mvc.Http.Status
import play.api.Logger
import db.AccountDto
import services.CryptoHelper


abstract class AbstractAuthHelper

trait AuthHelper {
  // Map[nonce, timestamp]
  var usedNonces: Map[String, Long] = Map()

  val authHeaderRealm = Play.application().configuration().getString("auth.header.realm")
  val authHeaderSignatureMethod = Play.application().configuration().getString("auth.header.signatureMethod")
  val authRetainedTimeframeInSeconds = Play.application().configuration().getLong("auth.retainedTimeframeInSeconds")

  val isAuthMock = Play.application().configuration().getBoolean("auth.isMock")

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

  def checkTokenRequestAuthentication(requestMethod: String, uri: String, headers: Headers): AuthenticationResult = {
    if (!isTokenRequestValid(headers))
      AuthenticationResult(Status.BAD_REQUEST)
    else {
      val authorizationHeader = getAuthorizationHeader(headers).get

      val timestampString = getSubHeader(authorizationHeader, "oauth_timestamp").get
      val clientIdentifier = getSubHeader(authorizationHeader, "oauth_consumer_key").get
      val nonce = getSubHeader(authorizationHeader, "oauth_nonce").get
      val signature = getSubHeader(authorizationHeader, "oauth_signature").get

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

  def checkRequestAuthentication(requestMethod: String, uri: String, headers: Headers): AuthenticationResult
  def getAuthorizationHeader(headers: Headers): Option[String]
  def getSubHeader(header: String, key: String): Option[String]

  protected def isTokenRequestValid(headers: Headers): Boolean = {
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
          realm.get == authHeaderRealm &&
            signatureMethod.get == authHeaderSignatureMethod &&
            isTimestampHeaderValid(timestampString.get)
        else
          false
    }
  }

  protected def buildSignatureFromRequestData(requestMethod: String, uri: String, clientIdentifier: String, tokenSharedSecret: Option[String]): Option[String] = {
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

  private def isTimestampHeaderValid(timestampString: String): Boolean = {
    try {
      timestampString.toLong
      true
    }
    catch {
      case nfe: NumberFormatException => false
    }
  }

  private def revokeTokenForClientIdentifier(clientIdentifier: String) {
    liveAccessTokens -= clientIdentifier
  }
}
