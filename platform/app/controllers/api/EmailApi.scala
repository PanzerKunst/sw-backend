package controllers.api

import services.JsonUtil
import play.api.mvc.{Action, Controller}
import db._
import models.clientside.ClientSideEmail
import play.api.libs.json.Json
import javax.security.auth.login.AccountNotFoundException
import scala.Some
import models.Email
import play.Play

object EmailApi extends Controller {
  val HTTP_STATUS_CODE_MISSING_REQUIRED_FIELDS = 520
  val HTTP_STATUS_CODE_TO_CC_BCC_ALL_EMPTY = 521
  val HTTP_STATUS_ACCOUNT_NOT_FOUND = 522
  val HTTP_STATUS_INCORRECT_STATUS = 523

  def create = Action(parse.json) {
    implicit request =>

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

  def get = Action {
    implicit request =>

      try {
        if (request.queryString.contains("accountId") && request.queryString.contains("status")) {
          val accountId = request.queryString.get("accountId").get.head.toLong
          val statuses = request.queryString.get("status").get.head.split(',').toList

          val matchingEmails = if (statuses.contains(Email.STATUS_TO_SEND) || statuses.contains(Email.STATUS_SENT))
            EmailDto.getEmailsFromAccount(accountId, statuses)
          else {
            EmailDto.getEmailsToAccount(accountId, statuses)
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
        else {
          throw new IncorrectEmailRequestException()
        }
      }
      catch {
        case iere: IncorrectEmailRequestException => Forbidden
        case e: Exception => InternalServerError(e.getMessage)
      }
  }

  def getOfId(id: Long) = Action {
    implicit request =>

      if (request.queryString.contains("accountId")) {
        val filters = Some(Map("id" -> id.toString))

        val matchingEmails = EmailDto.get(filters)

        if (matchingEmails.isEmpty)
          NoContent
        else {
          val matchingEmail = matchingEmails.head

          val clientSideEmail = new ClientSideEmail(
            matchingEmail,
            ToDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
            CcDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
            BccDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
            ReplyToDto.get(Some(Map("email_id" -> matchingEmail.id.toString))),
            ReferencesDto.get(Some(Map("email_id" -> matchingEmail.id.toString)))
          )

          val accountId = request.queryString.get("accountId").get.head.toLong
          val account = AccountDto.get(Some(Map("id" -> accountId.toString))).head
          val accountEmailAddress = account.username + "@" + Play.application().configuration().getString("email.domain")

          // We shouldn't return emails unrelated to the account
          val isAccountMatching = clientSideEmail.fromAccountId match {
            case Some(fromAccountId) => fromAccountId == accountId
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
      else {
        Forbidden
      }
  }
}

class IncorrectEmailRequestException extends Exception