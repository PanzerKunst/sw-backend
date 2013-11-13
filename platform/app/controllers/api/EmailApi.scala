package controllers.api

import services.{GlobalServices, JsonUtil}
import play.api.mvc.{Action, Controller}
import db._
import models.clientside.ClientSideEmail
import play.api.libs.json.Json
import javax.security.auth.login.AccountNotFoundException
import scala.Some
import models.Email
import play.Play
import play.mvc.Http

object EmailApi extends Controller {
  val HTTP_STATUS_CODE_MISSING_REQUIRED_FIELDS = 520
  val HTTP_STATUS_CODE_TO_CC_BCC_ALL_EMPTY = 521
  val HTTP_STATUS_ACCOUNT_NOT_FOUND = 522
  val HTTP_STATUS_INCORRECT_STATUS = 523

  def create = Action(parse.json) {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else {
        val clientSideEmail = JsonUtil.deserialize[ClientSideEmail](request.body.toString())

        clientSideEmail.validate match {
          case Some(errorMsg) =>
            if (errorMsg == ClientSideEmail.ERROR_MSG_MISSING_REQUIRED_FIELDS)
              Status(HTTP_STATUS_CODE_MISSING_REQUIRED_FIELDS)
            else if (errorMsg == ClientSideEmail.ERROR_MSG_TO_CC_BCC_ALL_EMPTY)
              Status(HTTP_STATUS_CODE_TO_CC_BCC_ALL_EMPTY)
            else
              Status(HTTP_STATUS_INCORRECT_STATUS)
          case None =>
            clientSideEmail.fromAccountId match {
              case None => BadRequest("'fromAccountId' must be specified")
              case Some(fromAccountId) =>
                // What is the username corresponding to clientSideEmail.fromAccountId?
                AccountDto.getOfId(fromAccountId) match {
                  case None => BadRequest("No account found for ID " + fromAccountId)
                  case Some(accountInDb) =>
                    val authorizationHeader = GlobalServices.authHelper.getAuthorizationHeader(request.headers).get
                    val clientIdentifier = GlobalServices.authHelper.getSubHeader(authorizationHeader, "oauth_consumer_key").get

                    if (clientIdentifier != accountInDb.username) {
                      Forbidden("The value of 'forAccountId' does not match your user session")
                    }
                    else {
                      try {
                        EmailDto.create(clientSideEmail) match {
                          case Some(id) =>
                            for (internetAddress <- clientSideEmail.to)
                              ToDto.create(id, internetAddress)

                            for (internetAddress <- clientSideEmail.cc)
                              CcDto.create(id, internetAddress)

                            for (internetAddress <- clientSideEmail.bcc)
                              BccDto.create(id, internetAddress)

                            for (messageId <- clientSideEmail.references)
                              ReferencesDto.create(id, messageId)

                            Created(id.toString)
                          case None => InternalServerError("Creation of an email did not return an ID!")
                        }
                      }
                      catch {
                        case anfe: AccountNotFoundException => Status(HTTP_STATUS_ACCOUNT_NOT_FOUND)
                        case e: Exception => InternalServerError(e.getMessage)
                      }
                    }
                }
            }
        }
      }
  }

  def get = Action {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else {
        try {
          if (request.queryString.contains("status")) {
            val statuses = request.queryString.get("status").get.head.split(',').toList

            val authorizationHeader = GlobalServices.authHelper.getAuthorizationHeader(request.headers).get
            val clientIdentifier = GlobalServices.authHelper.getSubHeader(authorizationHeader, "oauth_consumer_key").get

            AccountDto.getOfUsername(clientIdentifier) match {
              case None => BadRequest("No account found for this session")
              case Some(account) =>
                val matchingEmails = if (statuses.contains(Email.STATUS_TO_SEND) || statuses.contains(Email.STATUS_SENT))
                  EmailDto.getEmailsFromAccount(account.id.get, statuses)
                else {
                  EmailDto.getEmailsToAccount(account.id.get, statuses)
                }

                if (matchingEmails.isEmpty)
                  NoContent
                else {
                  val clientSideEmails = for (email <- matchingEmails) yield new ClientSideEmail(
                    email,
                    ToDto.get(Some(Map("email_id" -> email.id.toString))),
                    CcDto.get(Some(Map("email_id" -> email.id.toString))),
                    BccDto.get(Some(Map("email_id" -> email.id.toString))),
                    ReplyToDto.get(Some(Map("email_id" -> email.id.toString))),
                    ReferencesDto.get(Some(Map("email_id" -> email.id.toString)))
                  )
                  Ok(Json.toJson(clientSideEmails))
                }
            }
          }
          else {
            throw new IncorrectEmailRequestException()
          }
        }
        catch {
          case iere: IncorrectEmailRequestException => Forbidden
          case e: Exception => InternalServerError(e.getMessage)
        }
      }
  }

  def getOfId(id: Long) = Action {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else {
        EmailDto.getOfId(id) match {
          case None => NoContent
          case Some(matchingEmail) =>
            val clientSideEmail = new ClientSideEmail(
              matchingEmail,
              ToDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
              CcDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
              BccDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
              ReplyToDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
              ReferencesDto.get(Some(Map("email_id" -> matchingEmail.id.toString)))
            )

            val authorizationHeader = GlobalServices.authHelper.getAuthorizationHeader(request.headers).get
            val clientIdentifier = GlobalServices.authHelper.getSubHeader(authorizationHeader, "oauth_consumer_key").get

            AccountDto.getOfUsername(clientIdentifier) match {
              case None => BadRequest("No account found for this session")
              case Some(account) =>
                val accountEmailAddress = account.username + "@" + Play.application().configuration().getString("email.domain")

                // We shouldn't return emails unrelated to the account
                val isAccountMatching = clientSideEmail.fromAccountId match {
                  case Some(fromAccountId) => fromAccountId == account.id.get
                  case None => false
                }

                if (isAccountMatching ||
                  clientSideEmail.to.contains(accountEmailAddress) ||
                  clientSideEmail.cc.contains(accountEmailAddress) ||
                  clientSideEmail.bcc.contains(accountEmailAddress)) {

                  Ok(Json.toJson(clientSideEmail))
                }
                else {
                  Forbidden
                }
            }
        }
      }
  }

  def delete(id: Long) = Action {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else {
        EmailDto.getOfId(id) match {
          case None => BadRequest("No email found for id '" + id + "'")
          case Some(email) =>
            val authorizationHeader = GlobalServices.authHelper.getAuthorizationHeader(request.headers).get
            val clientIdentifier = GlobalServices.authHelper.getSubHeader(authorizationHeader, "oauth_consumer_key").get

            AccountDto.getOfUsername(clientIdentifier) match {
              case None => BadRequest("No account found for this session")
              case Some(account) =>
                val accountEmailAddress = account.username + "@" + Play.application().configuration().getString("email.domain")

                // We shouldn't delete emails unrelated to the account
                val isAccountMatching = email.fromAccountId match {
                  case Some(fromAccountId) => fromAccountId == account.id.get
                  case None => false
                }

                if (isAccountMatching) {
                  EmailDto.delete(email)
                  Ok
                }
                else {
                  val to = ToDto.get(Some(Map("email_id" -> email.id.toString)))
                  val cc = CcDto.get(Some(Map("email_id" -> email.id.toString)))
                  val bcc = BccDto.get(Some(Map("email_id" -> email.id.toString)))

                  if (to.contains(accountEmailAddress) ||
                    cc.contains(accountEmailAddress) ||
                    bcc.contains(accountEmailAddress)) {
                    EmailDto.delete(email)
                    Ok
                  }
                  else
                    Forbidden
                }
            }
        }
      }
  }
}

class IncorrectEmailRequestException extends Exception